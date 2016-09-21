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

package consulo.visualStudio.importProvider;

import consulo.visualStudio.VisualStudioImportTarget;
import consulo.visualStudio.util.VisualStudioProjectInfo;

/**
* @author VISTALL
* @since 09.06.2015
*/
public class VisualStudioImportItem
{
	private final VisualStudioProjectInfo myProjectInfo;
	private VisualStudioImportTarget myTarget;

	public VisualStudioImportItem(VisualStudioProjectInfo projectInfo, VisualStudioImportTarget target)
	{
		myProjectInfo = projectInfo;

		myTarget = target;
	}

	public VisualStudioProjectInfo getProjectInfo()
	{
		return myProjectInfo;
	}

	public VisualStudioImportTarget getTarget()
	{
		return myTarget;
	}

	public void setTarget(VisualStudioImportTarget target)
	{
		myTarget = target;
	}
}
