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

package com.atlassian.connector.eclipse.internal.crucible.ui;

import com.atlassian.connector.eclipse.internal.crucible.core.client.CrucibleClient;
import com.atlassian.connector.eclipse.ui.commons.ResourceSelectionTree.TreeViewMode;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 * 
 * @author Shawn Minto
 */
public class CrucibleUiPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.atlassian.connector.eclipse.crucible.ui";

	public static final String REVIEW_PERSPECTIVE_ID = PLUGIN_ID + ".reviewPerspective";

	public static final String COMMENT_VIEW_ID = PLUGIN_ID + ".commentView";

	public static final String EXPLORER_VIEW_ID = PLUGIN_ID + ".explorerView";

	public static final String PRODUCT_NAME = "Atlassian Crucible Connector";

	private static final String DEFAULT_PROJECT = "defaultProject";

	private static final String ALLOW_ANYONE_TO_JOIN = "allowAnyoneToJoin";

	private static final String START_REVIEW = "startReview";

	// The shared instance
	private static CrucibleUiPlugin plugin;

	private static CrucibleClient client;

	private ActiveReviewManager activeReviewManager;

	private SwitchingPerspectiveReviewActivationListener switchingPerspectivesListener;

	private AvatarImages avatarImages;

	/**
	 * The constructor
	 */
	public CrucibleUiPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		switchingPerspectivesListener = new SwitchingPerspectiveReviewActivationListener();
		activeReviewManager = new ActiveReviewManager(true);
		activeReviewManager.addReviewActivationListener(switchingPerspectivesListener);

		avatarImages = new AvatarImages();

		enableActiveReviewManager();

		plugin.getPreferenceStore().setDefault(CrucibleUiConstants.PREFERENCE_ACTIVATE_REVIEW,
				MessageDialogWithToggle.PROMPT);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		disableActiveReviewManager();

		activeReviewManager.dispose();
		activeReviewManager = null;

		avatarImages.dispose();
		avatarImages = null;

		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static CrucibleUiPlugin getDefault() {
		return plugin;
	}

	public ActiveReviewManager getActiveReviewManager() {
		return activeReviewManager;
	}

	/**
	 * Method for testing purposes
	 */
	public void disableActiveReviewManager() {
		if (activeReviewManager != null) {
			TasksUi.getTaskActivityManager().removeActivationListener(activeReviewManager);
		}
	}

	/**
	 * Method for testing purposes
	 */
	public void enableActiveReviewManager() {
		if (activeReviewManager != null) {
			TasksUi.getTaskActivityManager().addActivationListener(activeReviewManager);
		}
	}

	public boolean getPreviousChangesetReviewSelection() {
		return plugin.getPreferenceStore().getBoolean(CrucibleUiConstants.PREVIOUS_CHANGESET_REVIEW_SELECTION);
	}

	public boolean getPreviousPatchReviewSelection() {
		return plugin.getPreferenceStore().getBoolean(CrucibleUiConstants.PREVIOUS_PATCH_REVIEW_SELECTION);
	}

	public boolean getPreviousWorkspacePatchReviewSelection() {
		return plugin.getPreferenceStore().getBoolean(CrucibleUiConstants.PREVIOUS_WORKSPACE_PATCH_REVIEW_SELECTION);
	}

	public void setPreviousChangesetReviewSelection(boolean value) {
		plugin.getPreferenceStore().setValue(CrucibleUiConstants.PREVIOUS_CHANGESET_REVIEW_SELECTION, value);
	}

	public void setPreviousPatchReviewSelection(boolean value) {
		plugin.getPreferenceStore().setValue(CrucibleUiConstants.PREVIOUS_PATCH_REVIEW_SELECTION, value);
	}

	public void setPreviousWorkspacePatchReviewSelection(boolean value) {
		plugin.getPreferenceStore().setValue(CrucibleUiConstants.PREVIOUS_WORKSPACE_PATCH_REVIEW_SELECTION, value);
	}

	public TreeViewMode getResourcesTreeViewMode() {
		int mode = plugin.getPreferenceStore().getInt(CrucibleUiConstants.PREFERENCE_RESOURCE_TREE_VIEW_MODE);
		for (TreeViewMode treeMode : TreeViewMode.values()) {
			if (treeMode.ordinal() == mode) {
				return treeMode;
			}
		}

		return TreeViewMode.MODE_COMPRESSED_FOLDERS;
	}

	public void setResourcesTreeViewMode(TreeViewMode mode) {
		plugin.getPreferenceStore().setValue(CrucibleUiConstants.PREFERENCE_RESOURCE_TREE_VIEW_MODE, mode.ordinal());
	}

	public IDialogSettings getDialogSettingsSection(String name) {
		IDialogSettings dialogSettings = getDialogSettings();
		IDialogSettings section = dialogSettings.getSection(name);
		if (section == null) {
			section = dialogSettings.addNewSection(name);
		}
		return section;
	}

	public AvatarImages getAvatarsCache() {
		return this.avatarImages;
	}

	public void updateLastSelectedProject(TaskRepository repository, String projectKey) {
		repository.setProperty(DEFAULT_PROJECT, projectKey);
	}

	public String getLastSelectedProjectKey(TaskRepository repository) {
		return repository.getProperty(DEFAULT_PROJECT);
	}

	public boolean getAllowAnyoneOption(TaskRepository repository) {
		final String prop = repository.getProperty(ALLOW_ANYONE_TO_JOIN);
		return prop != null && Boolean.valueOf(prop);
	}

	public void updateAllowAnyoneOption(TaskRepository taskRepository, boolean allowAnyone) {
		taskRepository.setProperty(ALLOW_ANYONE_TO_JOIN, String.valueOf(allowAnyone));
	}

	public boolean getStartReviewOption(TaskRepository repository) {
		final String prop = repository.getProperty(START_REVIEW);
		return prop != null && Boolean.valueOf(prop);
	}

	public void updateStartReviewOption(TaskRepository taskRepository, boolean startReview) {
		taskRepository.setProperty(START_REVIEW, String.valueOf(startReview));
	}

	public static CrucibleClient getClient(TaskRepository taskRepository) {
		if (client == null) {
			client = new CrucibleClient();
		}
		return client;
	}

}
