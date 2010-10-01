/*******************************************************************************
 * Copyright (c) 2009 Atlassian and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Atlassian - initial API and implementation
 ******************************************************************************/

package com.atlassian.connector.eclipse.internal.crucible.ui.wizards;

import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class DefineRepositoryMappingButton {

//	private final AbstractCrucibleWizardPage page;

	private final Composite composite;

//	private final TaskRepository repository;

	private final Control defineMappingButton;

//	private String missingMapping; //N.A. to open source

	public DefineRepositoryMappingButton(final AbstractCrucibleWizardPage page, Composite composite,
			final TaskRepository repository) {
//		this.page = page;
		this.composite = composite;
//		this.repository = repository;

		this.defineMappingButton = createDefineRepositoryMappingsButton();

	}

	public Control getControl() {
		return defineMappingButton;
	}

	public void setMissingMapping(String mapping) {
//		this.missingMapping = mapping;  //N.A. to open source
	}

	private Control createDefineRepositoryMappingsButton() {

		Button updateData = new Button(composite, SWT.PUSH);
		updateData.setText("Define Repository Mappings");

		//Removed: Not applicable to open source
//		updateData.addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				FishEyePreferenceContextData data = page.isPageComplete() ? null : new FishEyePreferenceContextData(
//						missingMapping == null ? "" : missingMapping, repository);
//				final PreferenceDialog prefDialog = PreferencesUtil.createPreferenceDialogOn(page.getShell(),
//						SourceRepositoryMappingPreferencePage.ID, null, data);
//				if (prefDialog != null) {
//					if (prefDialog.open() == Window.OK) {
//						page.validatePage();
//					}
//				}
//			}
//		});
		return updateData;
	}
}