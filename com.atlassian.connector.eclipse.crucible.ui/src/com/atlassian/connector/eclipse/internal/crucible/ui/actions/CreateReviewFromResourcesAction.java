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

package com.atlassian.connector.eclipse.internal.crucible.ui.actions;

import com.atlassian.connector.eclipse.internal.crucible.ui.wizards.RepositorySelectionWizard;
import com.atlassian.connector.eclipse.internal.crucible.ui.wizards.ReviewWizard;
import com.atlassian.connector.eclipse.team.ui.ITeamUiResourceConnector;
import com.atlassian.connector.eclipse.ui.commons.ResourceEditorBean;
import com.atlassian.theplugin.commons.util.MiscUtil;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.mylyn.internal.provisional.commons.ui.WorkbenchUtil;
import org.eclipse.mylyn.internal.tasks.core.ITaskRepositoryFilter;
import org.eclipse.mylyn.internal.tasks.ui.wizards.SelectRepositoryPage;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.swt.widgets.Shell;

import java.util.Arrays;
import java.util.List;

/**
 * @author Jacek Jaroczynski
 */
@SuppressWarnings("restriction")
public class CreateReviewFromResourcesAction extends AbstractReviewFromResourcesAction {

	public CreateReviewFromResourcesAction() {
		super("Create Review Action");
	}

	protected void openReviewWizard(final ResourceEditorBean selection, final ITeamUiResourceConnector connector,
			boolean isPostCommit, final Shell shell) {
		SelectRepositoryPage selectRepositoryPage = new SelectRepositoryPage(ITaskRepositoryFilter.ALL) {
			@Override
			protected IWizard createWizard(TaskRepository taskRepository) {
				ReviewWizard wizard = new ReviewWizard(taskRepository, MiscUtil.buildHashSet(
						ReviewWizard.Type.ADD_RESOURCES, ReviewWizard.Type.ADD_COMMENT_TO_FILE));
				wizard.setRoots(connector, Arrays.asList(selection.getResource()));
				wizard.setFilesCommentData(Arrays.asList(selection));
				return wizard;
			}
		};

		WizardDialog wd = new WizardDialog(WorkbenchUtil.getShell(),
				new RepositorySelectionWizard(selectRepositoryPage));
		wd.setBlockOnOpen(true);
		wd.open();
	}

	protected void openReviewWizard(final ITeamUiResourceConnector teamConnector, final List<IResource> resources,
			boolean isCrucible21Required, Shell shell) {
		SelectRepositoryPage selectRepositoryPage = new SelectRepositoryPage(ITaskRepositoryFilter.ALL) {
			@Override
			protected IWizard createWizard(TaskRepository taskRepository) {
				ReviewWizard wizard = new ReviewWizard(taskRepository, MiscUtil.buildHashSet(
						ReviewWizard.Type.ADD_SCM_RESOURCES, ReviewWizard.Type.ADD_COMMENT_TO_FILE));
				wizard.setRoots(teamConnector, resources);
				return wizard;
			}
		};

		WizardDialog wd = new WizardDialog(shell, new RepositorySelectionWizard(selectRepositoryPage));
		wd.setBlockOnOpen(true);
		wd.open();
	}

}
