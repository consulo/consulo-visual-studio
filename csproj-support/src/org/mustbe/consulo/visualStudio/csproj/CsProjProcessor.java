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

package org.mustbe.consulo.visualStudio.csproj;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.consulo.lombok.annotations.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jetbrains.annotations.NotNull;
import org.mustbe.consulo.dotnet.DotNetTarget;
import org.mustbe.consulo.dotnet.module.roots.DotNetLibraryOrderEntryImpl;
import org.mustbe.consulo.microsoft.csharp.module.extension.MicrosoftCSharpMutableModuleExtension;
import org.mustbe.consulo.microsoft.dotnet.module.extension.MicrosoftDotNetMutableModuleExtension;
import org.mustbe.consulo.microsoft.dotnet.sdk.MicrosoftDotNetSdkType;
import org.mustbe.consulo.visualStudio.VisualStudioProjectProcessor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkTable;
import com.intellij.openapi.roots.ModifiableModuleRootLayer;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.impl.ModuleRootLayerImpl;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;

/**
 * @author VISTALL
 * @since 27.03.14
 */
@Logger
public class CsProjProcessor implements VisualStudioProjectProcessor
{
	private static final Namespace ourNamespace = Namespace.getNamespace("http://schemas.microsoft.com/developer/msbuild/2003");

	private static final String ourParentName = CsProjProcessor.class.getName();
	private static final String OutputType = "OutputType";
	private static final String TargetFrameworkVersion = "TargetFrameworkVersion";
	private static final String DefineConstants = "DefineConstants";
	private static final String DebugSymbols = "DebugSymbols";
	private static final String AllowUnsafeBlocks = "AllowUnsafeBlocks";


	@NotNull
	@Override
	public FileType getFileType()
	{
		return CsProjFileType.INSTANCE;
	}

	@Override
	public void processFile(@NotNull VirtualFile projectFile, @NotNull ModifiableRootModel modifiableRootModel)
	{
		try
		{
			List<Sdk> sdks = SdkTable.getInstance().getSdksOfType(MicrosoftDotNetSdkType.getInstance());

			Document document = JDOMUtil.loadDocument(projectFile.getInputStream());

			Map<String, PropertyGroup> groupMap = new LinkedHashMap<String, PropertyGroup>();
			Element rootElement = document.getRootElement();

			List<String> references = new ArrayList<String>();

			for(Element group : rootElement.getChildren("ItemGroup", ourNamespace))
			{
				for(Element ref : group.getChildren("Reference", ourNamespace))
				{
					references.add(ref.getAttributeValue("Include"));
				}
			}

			for(Element propertyGroup : rootElement.getChildren("PropertyGroup", ourNamespace))
			{
				String cond = propertyGroup.getAttributeValue("Condition");
				if(cond == null)
				{
					cond = ourParentName;
				}
				else
				{
					cond = cond.substring(" '$(Configuration)|$(Platform)' == '".length(), cond.lastIndexOf('|'));
				}

				PropertyGroup value = new PropertyGroup();
				groupMap.put(cond, value);

				String outputType = propertyGroup.getChildText(OutputType, ourNamespace);
				if("WinExe".equals(outputType))
				{
					value.put(OutputType, DotNetTarget.EXECUTABLE);
				}
				else if("Library".equals(outputType))
				{
					value.put(OutputType, DotNetTarget.LIBRARY);
				}

				String targetFrameworkVersion = propertyGroup.getChildText(TargetFrameworkVersion, ourNamespace);
				if(targetFrameworkVersion != null)
				{
					value.put(TargetFrameworkVersion, targetFrameworkVersion);
				}

				String defineConstants = propertyGroup.getChildText(DefineConstants, ourNamespace);
				if(defineConstants != null)
				{
					String[] split = defineConstants.split(";");
					value.put(DefineConstants, Arrays.asList(split));
				}

				String debugSymbols = propertyGroup.getChildText(DebugSymbols, ourNamespace);
				if(debugSymbols != null)
				{
					value.put(DebugSymbols, Boolean.parseBoolean(debugSymbols));
				}

				String unsafeBlocks = propertyGroup.getChildText(AllowUnsafeBlocks, ourNamespace);
				if(unsafeBlocks != null)
				{
					value.put(AllowUnsafeBlocks, Boolean.parseBoolean(unsafeBlocks));
				}
			}

			PropertyGroup own = groupMap.remove(ourParentName);

			for(PropertyGroup propertyGroup : groupMap.values())
			{
				propertyGroup.putAll(own.get());
			}

			modifiableRootModel.removeLayer(ModifiableRootModel.DEFAULT_LAYER_NAME, false);

			for(Map.Entry<String, PropertyGroup> groupEntry : groupMap.entrySet())
			{
				String key = groupEntry.getKey();
				PropertyGroup value = groupEntry.getValue();

				ModifiableModuleRootLayer layer = modifiableRootModel.addLayer(key, null, false);
				layer.addContentEntry(projectFile.getParent());

				MicrosoftDotNetMutableModuleExtension e = layer.getExtensionWithoutCheck(MicrosoftDotNetMutableModuleExtension.class);
				assert e != null;
				e.setEnabled(true);

				MicrosoftCSharpMutableModuleExtension c = layer.getExtensionWithoutCheck(MicrosoftCSharpMutableModuleExtension.class);
				assert c != null;
				c.setEnabled(true);


				e.setTarget(value.get(OutputType, DotNetTarget.EXECUTABLE));
				e.setAllowDebugInfo(value.get(DebugSymbols, Boolean.TRUE));
				c.setAllowUnsafeCode(value.get(AllowUnsafeBlocks, Boolean.TRUE));

				List<String> list = value.get(DefineConstants, Collections.<String>emptyList());
				e.getVariables().clear();
				e.getVariables().addAll(list);

				String version = MicrosoftDotNetSdkType.removeFirstCharIfIsV(value.get(TargetFrameworkVersion, "v2"));

				for(Sdk sdk : sdks)
				{
					if(sdk.getVersionString().startsWith(version))
					{
						e.getInheritableSdk().set(null, sdk);
						break;
					}
				}

				for(String reference : references)
				{
					layer.addOrderEntry(new DotNetLibraryOrderEntryImpl((ModuleRootLayerImpl) layer, reference));
				}
			}

			if(groupMap.isEmpty())
			{
				modifiableRootModel.addLayer(ModifiableRootModel.DEFAULT_LAYER_NAME, null, true);
			}
			else
			{
				modifiableRootModel.setCurrentLayer(ContainerUtil.getFirstItem(groupMap.keySet()));
			}
		}
		catch(Exception e)
		{
			LOGGER.error(e);
		}
	}
}
