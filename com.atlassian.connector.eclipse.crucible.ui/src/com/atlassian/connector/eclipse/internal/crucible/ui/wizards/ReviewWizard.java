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

import com.atlassian.connector.eclipse.internal.crucible.core.TaskRepositoryUtil;
import com.atlassian.connector.eclipse.internal.crucible.ui.CrucibleUiPlugin;
import com.atlassian.connector.eclipse.internal.crucible.ui.CrucibleUiUtil;
import com.atlassian.connector.eclipse.team.ui.AtlassianTeamUiPlugin;
import com.atlassian.connector.eclipse.team.ui.ICustomChangesetLogEntry;
import com.atlassian.connector.eclipse.team.ui.ITeamUiResourceConnector;
import com.atlassian.connector.eclipse.team.ui.LocalStatus;
import com.atlassian.connector.eclipse.team.ui.TeamUiUtils;
import com.atlassian.connector.eclipse.ui.commons.DecoratedResource;
import com.atlassian.connector.eclipse.ui.commons.ResourceEditorBean;
import com.atlassian.theplugin.commons.crucible.api.UploadItem;
import com.atlassian.theplugin.commons.crucible.api.model.BasicProject;
import com.atlassian.theplugin.commons.crucible.api.model.PermId;
import com.atlassian.theplugin.commons.crucible.api.model.Review;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.mylyn.internal.tasks.core.LocalTask;
import org.eclipse.mylyn.internal.tasks.ui.util.TasksUiInternal;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.mylyn.tasks.ui.TasksUiUtil;
import org.eclipse.mylyn.tasks.ui.wizards.NewTaskWizard;
import org.eclipse.ui.INewWizard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

/**
 * Wizard for creating a new review
 * 
 * @author Thomas Ehrnhoefer
 * @author Pawel Niewiadomski
 * @author Jacek Jaroczynski
 */
@SuppressWarnings("restriction")
public class ReviewWizard extends NewTaskWizard implements INewWizard {

	public enum Type {
		ADD_CHANGESET, ADD_PATCH, ADD_WORKSPACE_PATCH, ADD_SCM_RESOURCES, ADD_UPLOAD_ITEMS, ADD_RESOURCES, ADD_COMMENT_TO_FILE;
	}

	private CrucibleReviewDetailsPage detailsPage;

	private Review crucibleReview;

	private SelectScmChangesetsPage addChangeSetsPage;

	private CrucibleAddPatchPage addPatchPage;

//	private WorkspacePatchSelectionPage addWorkspacePatchPage;

	private DefineRepositoryMappingsPage defineMappingPage;

	private ResourceSelectionPage resourceSelectionPage;

	private final Set<Type> types;

	private SortedSet<ICustomChangesetLogEntry> preselectedLogEntries;

	private String previousPatch;

	private String previousPatchRepository;

	private final List<IResource> selectedWorkspaceResources = new ArrayList<IResource>();

	private IResource[] previousWorkspaceSelection;

	private List<UploadItem> uploadItems;

	private List<ResourceEditorBean> versionedCommentsToAdd = new ArrayList<ResourceEditorBean>();

	private SelectChangesetsFromCruciblePage addChangeSetsFromCruciblePage;

	private ITeamUiResourceConnector selectedWorkspaceTeamConnector;

	public ReviewWizard(TaskRepository taskRepository, Set<Type> types) {
		super(taskRepository, null);
		setWindowTitle("New Crucible Review");
		setNeedsProgressMonitor(true);
		this.types = types;
		this.selectedWorkspaceResources.addAll(Arrays.asList((IResource[]) ResourcesPlugin.getWorkspace()
				.getRoot()
				.getProjects()));
	}

	public ReviewWizard(Review review, Set<Type> types) {
		this(CrucibleUiUtil.getCrucibleTaskRepository(review), types);
		this.crucibleReview = review;
	}

	public ReviewWizard(Review review, Type type) {
		this(review, new HashSet<Type>(Arrays.asList(type)));
	}

	@Override
	public void addPages() {
		if (types.contains(Type.ADD_CHANGESET)) {
			addChangeSetsFromCruciblePage = new SelectChangesetsFromCruciblePage(getTaskRepository(),
					preselectedLogEntries);
			addPage(addChangeSetsFromCruciblePage);
		}

		if (types.contains(Type.ADD_PATCH)) {
			addPatchPage = new CrucibleAddPatchPage(getTaskRepository());
			addPage(addPatchPage);
		}

		// pre-commit
		if (types.contains(Type.ADD_WORKSPACE_PATCH)) {
//			addWorkspacePatchPage = new WorkspacePatchSelectionPage(getTaskRepository(), selectedWorkspaceResources);
//			addPage(addWorkspacePatchPage);
		}

		// post-commit for editor selection
		if (types.contains(Type.ADD_SCM_RESOURCES)) {

			if (selectedWorkspaceResources.size() > 0 && selectedWorkspaceResources.get(0) != null) {

				// single SCM integration selection supported
				final ITeamUiResourceConnector teamConnector = AtlassianTeamUiPlugin.getDefault()
						.getTeamResourceManager()
						.getTeamConnector(selectedWorkspaceResources.get(0));
				if (teamConnector == null) {
					MessageDialog.openInformation(getShell(), CrucibleUiPlugin.PRODUCT_NAME,
							"Cannot find Atlassian SCM Integration for '" + selectedWorkspaceResources.get(0).getName()
									+ "'.");
				} else {
					boolean missingMapping = false;
					Collection<String> scmPaths = new ArrayList<String>();
					// TODO use job below if there are plenty of resource (currently it is used for single resource)
					for (IResource resource : selectedWorkspaceResources) {
						try {
							LocalStatus status = teamConnector.getLocalRevision(resource);
							if (status.getScmPath() != null && status.getScmPath().length() > 0) {
								String scmPath = TeamUiUtils.getScmPath(resource, teamConnector);

								if (TaskRepositoryUtil.getMatchingSourceRepository(
										TaskRepositoryUtil.getScmRepositoryMappings(getTaskRepository()), scmPath) == null) {
									// we need to see mapping page
									missingMapping = true;
									scmPaths.add(scmPath);
								}

							}
						} catch (CoreException e) {
							// resource is probably not under version control
							// skip
						}
					}

					if (missingMapping) {
						defineMappingPage = new DefineRepositoryMappingsPage(scmPaths, getTaskRepository());
						addPage(defineMappingPage);
					}
				}
			}
		}

		// mixed review
		if (types.contains(Type.ADD_RESOURCES)) {
			resourceSelectionPage = new ResourceSelectionPage(getTaskRepository(), selectedWorkspaceTeamConnector,
					selectedWorkspaceResources);
			addPage(resourceSelectionPage);
		}

		// only add details page if review is not already existing
		if (crucibleReview == null) {
			detailsPage = new CrucibleReviewDetailsPage(getTaskRepository(), types.contains(Type.ADD_COMMENT_TO_FILE));
			addPage(detailsPage);
		}
	}

	@Override
	public boolean canFinish() {
		if (detailsPage != null) {
			return detailsPage.isPageComplete();
		}
		return super.canFinish();
	}

	@Override
	public boolean performFinish() {

		setErrorMessage(null);

		crucibleReview = detailsPage.getReview();
		LocalTask task = TasksUiInternal.createNewLocalTask("Review: " + crucibleReview.getSummary());
		crucibleReview.setPermId(new PermId(task.getTaskId()));

		if (detailsPage != null) {
			// save project selection
			final BasicProject selectedProject = detailsPage.getSelectedProject();
			CrucibleUiPlugin.getDefault().updateLastSelectedProject(getTaskRepository(),
					selectedProject != null ? selectedProject.getKey() : null);

			// save checkbox selections
			CrucibleUiPlugin.getDefault().updateAllowAnyoneOption(getTaskRepository(),
					detailsPage.isAllowAnyoneToJoin());
			CrucibleUiPlugin.getDefault().updateStartReviewOption(getTaskRepository(),
					detailsPage.isStartReviewImmediately());
		}

		if (addPatchPage != null) {
			String patchToAdd = addPatchPage.hasPatch() ? addPatchPage.getPatch() : null;
			String patchRepositoryToAdd = addPatchPage.hasPatch() ? addPatchPage.getPatchRepository() : null;

			if (patchToAdd != null && patchRepositoryToAdd != null && !patchToAdd.equals(previousPatch)
					&& !patchRepositoryToAdd.equals(previousPatchRepository)) {
				// create patch review
			}
		}

//		if (addWorkspacePatchPage != null) {
//			final IResource[] selection = addWorkspacePatchPage.getSelection();
//
//			if (selection != null && selection.length > 0 && !Arrays.equals(selection, previousWorkspaceSelection)
//					&& addWorkspacePatchPage.getSelectedTeamResourceConnector() != null) {
//				// create pre-commit review
//			}
//		}

		if (addChangeSetsPage != null || addChangeSetsFromCruciblePage != null) {
			final Map<String, Set<String>> changesetsToAdd = addChangeSetsPage != null ? addChangeSetsPage.getSelectedChangesets()
					: addChangeSetsFromCruciblePage.getSelectedChangesets();
			if (changesetsToAdd != null && changesetsToAdd.size() > 0) {
				// create review from changeset
			}
		}

		if (types.contains(Type.ADD_SCM_RESOURCES)) {
			if (selectedWorkspaceResources != null) {
				// create review from editor selection (post-commit)
			}
		}

		if (types.contains(Type.ADD_UPLOAD_ITEMS)) {
			if (uploadItems.size() > 0) {
				// create review from editor selection (pre-commit)
			}
		}

		if (resourceSelectionPage != null && types.contains(Type.ADD_RESOURCES)) {
			final List<DecoratedResource> resources = resourceSelectionPage.getSelection();
			if (resources != null && resources.size() > 0) {
				// create review from workbench selection (post- and pre-commit)
			}
		}

		TasksUiUtil.openTask(task);
		TasksUi.getTaskActivityManager().activateTask(task);
		CrucibleUiPlugin.getDefault()
				.getActiveReviewManager()
				.reviewAdded(task.getRepositoryUrl(), task.getTaskId(), crucibleReview);

		return true;
	}

	private void setErrorMessage(String message) {
		IWizardPage page = getContainer().getCurrentPage();
		if (page instanceof WizardPage) {
			((WizardPage) page).setErrorMessage(message != null ? message.replace("\n", " ") : null);
		}
	}

	public void setLogEntries(SortedSet<ICustomChangesetLogEntry> logEntries) {
		this.preselectedLogEntries = logEntries;
	}

	public void setRoots(ITeamUiResourceConnector teamConnector, List<IResource> list) {
		this.selectedWorkspaceResources.clear();
		this.selectedWorkspaceResources.addAll(list);
		this.selectedWorkspaceTeamConnector = teamConnector;
	}

	public void setUploadItems(List<UploadItem> uploadItems) {
		this.uploadItems = uploadItems;
	}

	public void setFilesCommentData(List<ResourceEditorBean> list) {
		this.versionedCommentsToAdd = list;
	}

}
