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

import com.atlassian.connector.eclipse.internal.crucible.core.CrucibleUtil;
import com.atlassian.connector.eclipse.internal.crucible.core.client.CrucibleClient;
import com.atlassian.connector.eclipse.internal.crucible.ui.util.EditorUtil;
import com.atlassian.connector.eclipse.team.ui.AtlassianTeamUiPlugin;
import com.atlassian.connector.eclipse.team.ui.CrucibleFile;
import com.atlassian.connector.eclipse.team.ui.ITeamUiResourceConnector;
import com.atlassian.connector.eclipse.team.ui.TeamUiResourceManager;
import com.atlassian.connector.eclipse.team.ui.TeamUiUtils;
import com.atlassian.connector.eclipse.team.ui.exceptions.UnsupportedTeamProviderException;
import com.atlassian.theplugin.commons.crucible.api.model.BasicProject;
import com.atlassian.theplugin.commons.crucible.api.model.BasicReview;
import com.atlassian.theplugin.commons.crucible.api.model.Comment;
import com.atlassian.theplugin.commons.crucible.api.model.CrucibleFileInfo;
import com.atlassian.theplugin.commons.crucible.api.model.Repository;
import com.atlassian.theplugin.commons.crucible.api.model.Review;
import com.atlassian.theplugin.commons.crucible.api.model.Reviewer;
import com.atlassian.theplugin.commons.crucible.api.model.User;
import com.atlassian.theplugin.commons.crucible.api.model.VersionedComment;
import com.atlassian.theplugin.commons.util.MiscUtil;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.internal.tasks.core.LocalRepositoryConnector;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class for the UI
 * 
 * @author Shawn Minto
 * @author Wojciech Seliga
 */
public final class CrucibleUiUtil {

	private CrucibleUiUtil() {
	}

	public static TaskRepository getCrucibleTaskRepository(String repositoryUrl) {
		return TasksUi.getRepositoryManager().getRepository(LocalRepositoryConnector.CONNECTOR_KIND,
				LocalRepositoryConnector.REPOSITORY_URL);
	}

	public static ITask getCrucibleTask(TaskRepository taskRepository, String taskId) {
		if (taskRepository != null && taskId != null) {
			return TasksUi.getRepositoryModel().getTask(taskRepository, taskId);
		}
		return null;
	}

	public static TaskRepository getCrucibleTaskRepository(BasicReview review) {
		if (review != null) {
			String repositoryUrl = review.getServerUrl();
			if (repositoryUrl != null) {
				return getCrucibleTaskRepository(repositoryUrl);
			}
		}
		return null;
	}

	public static CrucibleClient getClient(BasicReview review) {
		return CrucibleUiPlugin.getClient(getCrucibleTaskRepository(review));
	}

	public static ITask getCrucibleTask(Review review) {
		if (review != null) {
			TaskRepository taskRepository = getCrucibleTaskRepository(review);
			String taskId = CrucibleUtil.getTaskIdFromPermId(review.getPermId().getId());
			if (taskRepository != null && taskId != null) {
				return getCrucibleTask(taskRepository, taskId);
			}
		}

		return null;
	}

	public static boolean hasCurrentUserCompletedReview(Review review) {
		String currentUser = getCurrentUsername(review);
		return CrucibleUtil.isUserCompleted(currentUser, review);
	}

	public static String getCurrentUsername(Review review) {
		return getCurrentUsername(CrucibleUiUtil.getCrucibleTaskRepository(review));
	}

	public static User getCurrentCachedUser(TaskRepository repository) {
		return getCachedUser(getCurrentUsername(repository), repository);
	}

	public static User getCurrentCachedUser(Review review) {
		TaskRepository repository = CrucibleUiUtil.getCrucibleTaskRepository(review);
		return getCachedUser(getCurrentUsername(repository), repository);
	}

	private static boolean hasReviewerCompleted(Review review, String username) {
		for (Reviewer r : review.getReviewers()) {
			if (r.getUsername().equals(username)) {
				return r.isCompleted();
			}
		}
		return false;
	}

	public static Reviewer createReviewerFromCachedUser(Review review, User user) {
		boolean completed = hasReviewerCompleted(review, user.getUsername());
		return new Reviewer(user.getUsername(), user.getDisplayName(), completed);
	}

	public static String getCurrentUsername(TaskRepository repository) {
		/*
		 * String currentUser = CrucibleCorePlugin.getRepositoryConnector() .getClientManager() .getClient(repository)
		 * .getUserName();
		 */
		return repository.getUserName();
	}

	public static User getCachedUser(String userName, TaskRepository repository) {
		if (userName != null) {
			for (User user : getCachedUsers(repository)) {
				if (userName.equals(user.getUsername())) {
					return user;
				}
			}
		}
		return null;
	}

	public static boolean isUserReviewer(String userName, Review review) {
		for (Reviewer reviewer : review.getReviewers()) {
			if (reviewer.getUsername().equals(userName)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isCurrentUserReviewer(Review review) {
		return isUserReviewer(CrucibleUiUtil.getCurrentUsername(review), review);
	}

	public static boolean isFilePartOfActiveReview(CrucibleFile crucibleFile) {
		Review activeReview = CrucibleUiPlugin.getDefault().getActiveReviewManager().getActiveReview();
		return isFilePartOfReview(crucibleFile, activeReview);
	}

	public static boolean isFilePartOfReview(CrucibleFile crucibleFile, Review review) {
		if (review == null || crucibleFile == null || crucibleFile.getCrucibleFileInfo() == null
				|| crucibleFile.getCrucibleFileInfo().getFileDescriptor() == null) {
			return false;
		}
		for (CrucibleFileInfo fileInfo : review.getFiles()) {
			if (fileInfo != null
					&& fileInfo.getFileDescriptor() != null
					&& fileInfo.getFileDescriptor()
							.getUrl()
							.equals(crucibleFile.getCrucibleFileInfo().getFileDescriptor().getUrl())
					&& fileInfo.getFileDescriptor()
							.getRevision()
							.equals(crucibleFile.getCrucibleFileInfo().getFileDescriptor().getRevision())) {
				return true;
			}
		}
		return false;
	}

	public static Set<User> getCachedUsers(Review review) {
		return getCachedUsers(getCrucibleTaskRepository(review));
	}

	public static Set<User> getCachedUsers(TaskRepository repository) {
		final Set<User> users;
		users = new HashSet<User>();
		users.add(new User("User A"));
		users.add(new User("User B"));
		return users;
	}

	public static Set<Repository> getCachedRepositories(TaskRepository repository) {
		Set<Repository> repositories;
		repositories = new HashSet<Repository>();
		return repositories;
	}

	public static Collection<BasicProject> getCachedProjects(TaskRepository repository) {
		final Set<BasicProject> projects;
		projects = new HashSet<BasicProject>();
		projects.add(new BasicProject("id", "key", "Review Project"));
		return projects;
	}

	public static BasicProject getCachedProject(TaskRepository repository, String projectKey) {
		return getCachedProjects(repository).iterator().next();
	}

	public static Collection<User> getUsersFromUsernames(TaskRepository taskRepository, Collection<String> usernames) {
		Set<User> users = CrucibleUiUtil.getCachedUsers(taskRepository);
		Set<User> result = MiscUtil.buildHashSet();
		for (User user : users) {
			if (usernames.contains(user.getUsername())) {
				result.add(user);
			}
		}
		return result;
	}

	public static boolean canModifyComment(Review review, Comment comment) {
		return true;
	}

	public static boolean canMarkAsReadOrUnread(Review review, Comment comment) {
		return true;
	}

	public static Set<String> getUsernamesFromUsers(Collection<? extends User> users) {
		final Set<String> userNames = new HashSet<String>();
		for (User user : users) {
			userNames.add(user.getUsername());
		}
		return userNames;
	}

	public static Set<Reviewer> getAllCachedUsersAsReviewers(TaskRepository taskRepository) {
		return toReviewers(CrucibleUiUtil.getCachedUsers(taskRepository));
	}

	public static Set<Reviewer> toReviewers(Collection<User> users) {
		Set<Reviewer> allReviewers = new HashSet<Reviewer>();
		for (User user : users) {
			allReviewers.add(new Reviewer(user.getUsername(), user.getDisplayName(), false));
		}
		return allReviewers;
	}

	public static Set<User> toUsers(Collection<Reviewer> users) {
		Set<User> res = new HashSet<User>();
		for (Reviewer user : users) {
			res.add(new User(user.getUsername(), user.getDisplayName(), user.getAvatarUrl()));
		}
		return res;
	}

	public static void focusOnComment(IEditorPart editor, CrucibleFile crucibleFile, VersionedComment versionedComment) {
		if (editor instanceof ITextEditor) {
			ITextEditor textEditor = ((ITextEditor) editor);
			if (versionedComment != null) {
				EditorUtil.selectAndReveal(textEditor, versionedComment, crucibleFile.getSelectedFile());
			}
		}
	}

	/**
	 * Gets file from review (both pre- or post-commit)
	 * 
	 * @param resource
	 * @param review
	 * @return
	 */
	public static CrucibleFile getCrucibleFileFromResource(IResource resource, Review review, IProgressMonitor monitor) {
		CrucibleFile cruFile = getCruciblePostCommitFile(resource, review);

		if (cruFile != null) {
			return cruFile;
		}

		try {
			return getCruciblePreCommitFile(resource, review, monitor);
		} catch (CoreException e) {
			StatusHandler.log(new Status(IStatus.WARNING, CrucibleUiPlugin.PRODUCT_NAME,
					"Cannot find pre-commit file for selected resource.", e));
		}

		return null;
	}

	/**
	 * Gets post-commit file form review
	 * 
	 * @param resource
	 * @param review
	 * @return
	 */
	public static CrucibleFile getCruciblePostCommitFile(IResource resource, Review review) {
		if (review == null || !(resource instanceof IFile)) {
			return null;
		}

		IFile file = (IFile) resource;

		TeamUiResourceManager teamResourceManager = AtlassianTeamUiPlugin.getDefault().getTeamResourceManager();

		for (ITeamUiResourceConnector connector : teamResourceManager.getTeamConnectors()) {
			if (connector.isEnabled() && connector.canHandleFile(file)) {
				CrucibleFile fileInfo;
				try {
					fileInfo = connector.getCrucibleFileFromReview(review, file);
				} catch (UnsupportedTeamProviderException e) {
					return null;
				}
				if (fileInfo != null) {
					return fileInfo;
				}
			}
		}

		try {
			CrucibleFile crucibleFile = TeamUiUtils.getDefaultConnector().getCrucibleFileFromReview(review, file);
			if (crucibleFile != null) {
				return crucibleFile;
			}
		} catch (UnsupportedTeamProviderException e) {
			// ignore
		}

		for (CrucibleFileInfo crucibleFile : review.getFiles()) {
			if (file.getName().equals(crucibleFile.getPermId().getId())) {
				return new CrucibleFile(crucibleFile, true);
			}
		}
		return null;
	}

	/**
	 * Gets pre-commit file form review
	 * 
	 * @param resource
	 * @param review
	 * @return
	 * @throws CoreException
	 */
	public static CrucibleFile getCruciblePreCommitFile(final IResource resource, Review review,
			IProgressMonitor monitor) throws CoreException {

		if (review == null || !(resource instanceof IFile)) {
			return null;
		}

		IFile file = (IFile) resource;

//		String localFileUrl = StringUtil.removeLeadingAndTrailingSlashes(file.getFullPath().toString());
		String localFileUrl = file.getFullPath().toString();

		List<CrucibleFile> matchingFiles = new ArrayList<CrucibleFile>();

		for (CrucibleFileInfo cruFile : review.getFiles()) {
//			String newFileUrl = StringUtil.removeLeadingAndTrailingSlashes(cruFile.getFileDescriptor().getUrl());
//			String oldFileUrl = StringUtil.removeLeadingAndTrailingSlashes(cruFile.getOldFileDescriptor().getUrl());
			String newFileUrl = cruFile.getFileDescriptor().getUrl();
			String oldFileUrl = cruFile.getOldFileDescriptor().getUrl();

			if (newFileUrl != null && newFileUrl.equals(localFileUrl)) {
				matchingFiles.add(new CrucibleFile(cruFile, false));
			} else if (oldFileUrl != null && oldFileUrl.equals(localFileUrl)) {
				matchingFiles.add(new CrucibleFile(cruFile, true));
			}
		}

		if (matchingFiles.size() > 0) {
			CrucibleClient client = CrucibleUiUtil.getClient(review);
			TaskRepository repository = CrucibleUiUtil.getCrucibleTaskRepository(review);

			for (final CrucibleFile cruFile : matchingFiles) {
				final String url = cruFile.getSelectedFile().getContentUrl();
				if (url == null || url.length() == 0) {
					StatusHandler.log(new Status(IStatus.WARNING, CrucibleUiPlugin.PRODUCT_NAME,
							"Cannot find pre-commit file for selected resource. Matching review item content url is empty"));
					continue;
				}
//				Boolean ret = client.execute(new RemoteOperation<Boolean, CrucibleServerFacade2>(monitor, repository) {
//
//					@Override
//					public Boolean run(CrucibleServerFacade2 server, ConnectionCfg serverCfg, IProgressMonitor monitor)
//							throws RemoteApiException, ServerPasswordNotProvidedException {
//
//						final byte[] content = OpenVirtualFileJob.getContent(url, server.getSession(serverCfg),
//								serverCfg.getUrl());
//
//						if (content == null) {
//							return false;
//						}
//
//						File localFile;
//						try {
//							localFile = OpenVirtualFileJob.createTempFile(cruFile.getSelectedFile().getName(), content);
//
//							if (FileUtils.contentEquals(localFile, resource.getRawLocation().toFile())) {
//								return true;
//							}
//						} catch (IOException e) {
//							StatusHandler.log(new Status(
//									IStatus.ERROR,
//									CrucibleUiPlugin.PRODUCT_NAME,
//									"Cannot create local temporary file. Cannot compare selected resource with review item.",
//									e));
//						}
//						return false;
//					}
//				}, true);

//				if (ret) {
//					return cruFile;
//				}
			}
		}

		return null;
	}

	public static String getDisplayNameOrUsername(User user) {
		return user.getDisplayName() == null || "".equals(user.getDisplayName()) ? user.getUsername()
				: user.getDisplayName();
	}

	public static boolean hasCachedData(TaskRepository taskRepository) {
		// ignore
		return false;
	}

	public static void updateTaskRepositoryCache(TaskRepository taskRepository, IWizardContainer container,
			IWizardPage selectChangesetsFromCruciblePage) {
		// ignore

	}

}
