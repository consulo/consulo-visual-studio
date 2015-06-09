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

package org.mustbe.consulo.visualStudio.importProvider.ui;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.util.List;

import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import org.jdesktop.swingx.autocomplete.ComboBoxCellEditor;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.visualStudio.VisualStudioImportTarget;
import org.mustbe.consulo.visualStudio.importProvider.VisualStudioImportBuilder;
import org.mustbe.consulo.visualStudio.importProvider.VisualStudioImportItem;
import com.intellij.ide.util.newProjectWizard.ProjectNameStep;
import com.intellij.ide.util.newProjectWizard.modes.WizardMode;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;

/**
 * @author VISTALL
 * @since 09.06.2015
 */
public class VisualStudioSetupTargetStep extends ProjectNameStep
{
	private List<VisualStudioImportItem> myItems;

	public VisualStudioSetupTargetStep(WizardContext wizardContext, @Nullable WizardMode mode, VirtualFile fileByPath)
	{
		super(wizardContext, mode);

		myItems = VisualStudioImportBuilder.loadItems(fileByPath);

		ColumnInfo<VisualStudioImportItem, String> nameColumn = new ColumnInfo<VisualStudioImportItem, String>("Name")
		{
			@Nullable
			@Override
			public String valueOf(VisualStudioImportItem tableItem)
			{
				return tableItem.getProjectInfo().getName();
			}
		};

		ColumnInfo<VisualStudioImportItem, VisualStudioImportTarget> targetColumn = new ColumnInfo<VisualStudioImportItem,
				VisualStudioImportTarget>("Framework")
		{
			@Nullable
			@Override
			public VisualStudioImportTarget valueOf(VisualStudioImportItem tableItem)
			{
				return tableItem.getTarget();
			}

			@Override
			public boolean isCellEditable(VisualStudioImportItem tableItem)
			{
				return true;
			}

			@Override
			public void setValue(VisualStudioImportItem tableItem, VisualStudioImportTarget value)
			{
				tableItem.setTarget(value);
			}

			@Override
			public int getWidth(JTable table)
			{
				return 100;
			}

			@Nullable
			@Override
			public TableCellEditor getEditor(VisualStudioImportItem o)
			{
				ComboBox comboBox = new ComboBox(VisualStudioImportTarget.getAvailableTargets());
				return new ComboBoxCellEditor(comboBox);
			}

			@Override
			public Class getColumnClass()
			{
				return VisualStudioImportTarget.class;
			}
		};
		ListTableModel<VisualStudioImportItem> tableModel = new ListTableModel<VisualStudioImportItem>(new ColumnInfo[]{
				nameColumn,
				targetColumn
		}, myItems);
		TableView<VisualStudioImportItem> tableItemTableView = new TableView<VisualStudioImportItem>(tableModel);

		myAdditionalContentPanel.add(ScrollPaneFactory.createScrollPane(tableItemTableView), new GridBagConstraints(0, GridBagConstraints.RELATIVE,
				1, 1, 1, 1, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
	}

	@Override
	public void updateDataModel()
	{
		super.updateDataModel();
		VisualStudioImportBuilder projectBuilder = (VisualStudioImportBuilder) myWizardContext.getProjectBuilder();
		assert projectBuilder != null;
		projectBuilder.setImportItems(myItems);
	}
}
