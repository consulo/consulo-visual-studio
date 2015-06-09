/*
 * Copyright 2013-2014 must-be.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mustbe.consulo.visualStudio.importProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.Icon;

import org.bromix.msbuild.Item;
import org.bromix.msbuild.ItemGroup;
import org.consulo.module.extension.MutableModuleExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.RequiredReadAction;
import org.mustbe.consulo.dotnet.module.extension.DotNetMutableModuleExtension;
import org.mustbe.consulo.dotnet.module.roots.DotNetLibraryOrderEntryImpl;
import org.mustbe.consulo.visualStudio.VisualStudioIcons;
import org.mustbe.consulo.visualStudio.VisualStudioImportTarget;
import org.mustbe.consulo.visualStudio.VisualStudioLanguageImportProvider;
import org.mustbe.consulo.visualStudio.util.VisualStudioProjectInfo;
import org.mustbe.consulo.visualStudio.util.VisualStudioSolutionParser;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkTable;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.roots.ModifiableModuleRootLayer;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.impl.ModuleRootLayerImpl;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.ModifiableArtifactModel;
import com.intellij.projectImport.ProjectImportBuilder;
import com.intellij.util.KeyedLazyInstanceEP;
import com.intellij.util.containers.ContainerUtil;

/**
 * @author VISTALL
 * @since 27.03.14
 */
public class VisualStudioImportBuilder extends ProjectImportBuilder<Object>
{
	private List<VisualStudioImportItem> myImportItems;

	@NotNull
	@Override
	public String getName()
	{
		return "Visual Studio";
	}

	@Override
	public Icon getIcon()
	{
		return VisualStudioIcons.VisualStudio;
	}

	@Override
	public List<Object> getList()
	{
		return null;
	}

	@Override
	public boolean isMarked(Object element)
	{
		return false;
	}

	@Override
	public void setList(List<Object> list) throws ConfigurationException
	{

	}

	@Override
	public void setOpenProjectSettingsAfter(boolean on)
	{

	}

	@Nullable
	@Override
	@RequiredReadAction
	public List<Module> commit(Project project, ModifiableModuleModel old, ModulesProvider modulesProvider, ModifiableArtifactModel artifactModel)
	{
		List<Module> modules;
		try
		{
			VirtualFile solutionFile = LocalFileSystem.getInstance().findFileByPath(getFileToImport());
			assert solutionFile != null;

			List<VisualStudioImportItem> importItems = getImportItems(solutionFile);

			VirtualFile parent = solutionFile.getParent();

			modules = new ArrayList<Module>(importItems.size());

			final ModifiableModuleModel modifiableModuleModel = old == null ? ModuleManager.getInstance(project).getModifiableModel() : old;

			final ModifiableRootModel mainModuleModel = createModuleWithSingleContent(parent.getName() + " (Solution)", parent,
					modifiableModuleModel);
			modules.add(mainModuleModel.getModule());
			new WriteAction<Object>()
			{
				@Override
				protected void run(Result<Object> objectResult) throws Throwable
				{
					mainModuleModel.commit();
				}
			}.execute();

			for(VisualStudioImportItem o : importItems)
			{
				VirtualFile projectFile = LocalFileSystem.getInstance().findFileByIoFile(o.getProjectInfo().getFile());
				if(projectFile == null)
				{
					continue;
				}

				Module module = modifiableModuleModel.newModule(o.getProjectInfo().getName(), null);
				final ModifiableRootModel modifiableRootModel = ModuleRootManager.getInstance(module).getModifiableModel();
				modules.add(modifiableRootModel.getModule());

				setupModule(o, modifiableRootModel);

				new WriteAction<Object>()
				{
					@Override
					protected void run(Result<Object> objectResult) throws Throwable
					{
						modifiableRootModel.commit();
					}
				}.execute();
			}

			if(modifiableModuleModel != old)
			{
				new WriteAction<Object>()
				{
					@Override
					protected void run(Result<Object> objectResult) throws Throwable
					{
						modifiableModuleModel.commit();
					}
				}.execute();
			}
		}
		finally
		{
			myImportItems = null;
		}
		return modules;
	}

	private void setupModule(VisualStudioImportItem importItem, ModifiableRootModel model)
	{
		ModifiableModuleRootLayer layer = (ModifiableModuleRootLayer) model.getCurrentLayer();

		// setup layer
		DotNetMutableModuleExtension<?> extension = layer.getExtensionWithoutCheck(importItem.getTarget().getFrameworkExtensionId());
		if(extension == null)
		{
			return;
		}

		Class<? extends SdkType> sdkTypeClass = extension.getSdkTypeClass();

		extension.setEnabled(true);

		SdkType sdkType = SdkType.EP_NAME.findExtension(sdkTypeClass);
		if(sdkType != null)
		{
			SdkTable sdkTable = SdkTable.getInstance();
			List<Sdk> sdksOfType = sdkTable.getSdksOfType(sdkType);

			// we need sort predefined first
			ContainerUtil.sort(sdksOfType, new Comparator<Sdk>()
			{
				@Override
				public int compare(Sdk o1, Sdk o2)
				{
					return getWeight(o2) - getWeight(o1);
				}

				private int getWeight(Sdk sdk)
				{
					return sdk.isPredefined() ? 100 : 0;
				}
			});
		}

		VisualStudioProjectInfo projectInfo = importItem.getProjectInfo();
		File file = projectInfo.getFile();
		String fileExtension = FileUtilRt.getExtension(file.getName());

		for(KeyedLazyInstanceEP<VisualStudioLanguageImportProvider> instanceEP : VisualStudioLanguageImportProvider.EP_NAME.getExtensions())
		{
			String key = instanceEP.getKey();
			if(fileExtension.equalsIgnoreCase(key))
			{
				MutableModuleExtension<?> languageExtension = layer.getExtensionWithoutCheck(instanceEP.getInstance().getLanguageModuleExtensionId
						(importItem.getTarget()));
				if(languageExtension != null)
				{
					languageExtension.setEnabled(true);
				}
				break;
			}
		}

		org.bromix.msbuild.Project project = projectInfo.getProject();
		for(org.bromix.msbuild.Element element : project.getChildren())
		{
			if(element instanceof ItemGroup)
			{
				List<org.bromix.msbuild.Element> children = ((ItemGroup) element).getChildren();
				for(org.bromix.msbuild.Element child : children)
				{
					if(child instanceof Item)
					{
						String name = ((Item) child).getName();
						if("Reference".equals(name))
						{
							layer.addOrderEntry(new DotNetLibraryOrderEntryImpl((ModuleRootLayerImpl) layer, ((Item) child).getInclude()));
						}
						else if("Compile".equals(name))
						{
							File contentFile = new File(file.getParent(), ((Item) child).getInclude());
							VirtualFile contentVirtualFile = LocalFileSystem.getInstance().findFileByIoFile(contentFile);
							if(contentVirtualFile != null)
							{
								layer.addContentEntry(contentVirtualFile);
							}
						}
					}
				}
			}
		}

	}

	private ModifiableRootModel createModuleWithSingleContent(String name, VirtualFile dir, ModifiableModuleModel modifiableModuleModel)
	{
		Module module = modifiableModuleModel.newModule(name, dir.getPath());

		ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
		ModifiableRootModel modifiableModel = moduleRootManager.getModifiableModel();
		modifiableModel.addContentEntry(dir);

		return modifiableModel;
	}

	@NotNull
	public List<VisualStudioImportItem> getImportItems(VirtualFile file)
	{
		if(myImportItems == null)
		{
			myImportItems = loadItems(file);
		}
		return myImportItems;
	}

	public void setImportItems(@NotNull List<VisualStudioImportItem> importItems)
	{
		myImportItems = importItems;
	}

	@NotNull
	public static List<VisualStudioImportItem> loadItems(VirtualFile file)
	{
		List<VisualStudioProjectInfo> studioSolution = VisualStudioSolutionParser.parse(file);

		List<VisualStudioImportItem> visualStudioImportItems = new ArrayList<VisualStudioImportItem>(studioSolution.size());
		for(VisualStudioProjectInfo visualStudioProjectInfo : studioSolution)
		{
			visualStudioImportItems.add(new VisualStudioImportItem(visualStudioProjectInfo, VisualStudioImportTarget._NET));
		}

		return visualStudioImportItems;
	}

	public void setupItems(List<VisualStudioImportItem> upItems)
	{
		myImportItems = upItems;
	}
}
