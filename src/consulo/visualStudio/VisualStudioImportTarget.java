/*
 * Copyright 2013-2015 must-be.org
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

package consulo.visualStudio;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import com.intellij.util.containers.ContainerUtil;
import consulo.lombok.annotations.ArrayFactoryFields;
import consulo.module.extension.ModuleExtensionProviderEP;

/**
 * @author VISTALL
 * @since 09.06.2015
 */
@ArrayFactoryFields
public enum VisualStudioImportTarget
{
	_NET("microsoft-dotnet"),
	Mono("mono-dotnet");

	private final String myPresentableName;
	private final String myFrameworkExtensionId;

	VisualStudioImportTarget(String frameworkExtensionId)
	{
		myFrameworkExtensionId = frameworkExtensionId;
		myPresentableName = name().replace("_", ".");
	}

	@NotNull
	public static VisualStudioImportTarget[] getAvailableTargets()
	{
		VisualStudioImportTarget[] values = values();
		List<VisualStudioImportTarget> list = new ArrayList<VisualStudioImportTarget>(values.length);
		for(VisualStudioImportTarget visualStudioImportTarget : values)
		{
			ModuleExtensionProviderEP providerEP = ModuleExtensionProviderEP.findProviderEP(visualStudioImportTarget.getFrameworkExtensionId());
			if(providerEP != null)
			{
				list.add(visualStudioImportTarget);
			}
		}
		return ContainerUtil.toArray(list, VisualStudioImportTarget.ARRAY_FACTORY);
	}

	public String getFrameworkExtensionId()
	{
		return myFrameworkExtensionId;
	}

	@Override
	public String toString()
	{
		return myPresentableName;
	}
}
