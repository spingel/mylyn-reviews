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

package com.atlassian.connector.eclipse.internal.crucible.core.client;

import com.atlassian.theplugin.commons.crucible.api.model.BasicReview;
import com.atlassian.theplugin.commons.crucible.api.model.CrucibleAction;
import com.atlassian.theplugin.commons.crucible.api.model.CrucibleVersionInfo;
import com.atlassian.theplugin.commons.crucible.api.model.Review;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskData;

/**
 * Bridge between Mylyn and the ACC API's
 * 
 * @author Shawn Minto
 * @author Thomas Ehrnhoefer
 * @author Wojciech Seliga
 */
public class CrucibleClient {

	public CrucibleClient() {
	}

	public TaskData getTaskData(TaskRepository taskRepository, final String taskId, IProgressMonitor monitor)
			throws CoreException {
		return null;
	}

	public Review getReview(TaskRepository repository, String taskId, boolean getWorkingCopy, IProgressMonitor monitor)
			throws CoreException {
		return null;
	}

	public CrucibleVersionInfo updateVersionInfo(IProgressMonitor monitor, TaskRepository taskRepository)
			throws CoreException {
		return null;
	}

	public void updateRepositoryData(IProgressMonitor monitor, TaskRepository taskRepository) throws CoreException {
	}

	public void updateProjectDetails(IProgressMonitor monitor, TaskRepository taskRepository, final String projectKey)
			throws CoreException {
	}

	public Review changeReviewState(final BasicReview review, final CrucibleAction action, TaskRepository repository,
			IProgressMonitor progressMonitor) throws CoreException {
		return null;
	}

}
