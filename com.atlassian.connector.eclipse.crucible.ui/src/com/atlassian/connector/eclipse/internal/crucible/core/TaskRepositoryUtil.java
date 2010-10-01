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

package com.atlassian.connector.eclipse.internal.crucible.core;

import com.atlassian.theplugin.commons.util.MiscUtil;

import org.eclipse.mylyn.tasks.core.TaskRepository;

import java.net.URI;
import java.util.Map;

public final class TaskRepositoryUtil {

	private static final String FAILED_TO_DE_SERIALIZE_MAPPINGS = "Failed to de-serialize mappings";

	private static final String TASK_REPOSITORY_SCM_MAPPINGS_KEY = "com.atlassian.connector.eclipse.crucible.core.scmRepositoryMappings";

	private TaskRepositoryUtil() {
	}

	/**
	 * Returns a mapping between SCM urls and Crucible/FishEye source repositories. It's is persisted as a
	 * TaskRepository property.
	 * 
	 * @param taskRepository
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, String> getScmRepositoryMappings(TaskRepository taskRepository) {
		String property = taskRepository.getProperty(TASK_REPOSITORY_SCM_MAPPINGS_KEY);
		return MiscUtil.buildHashMap();
	}

	public static void setScmRepositoryMappings(TaskRepository taskRepository, Map<String, String> mappings) {
	}

	public static Map.Entry<String, String> getMatchingSourceRepository(Map<String, String> repositories, String scmPath) {

		Map.Entry<String, String> matching = null;
		for (Map.Entry<String, String> prefix : repositories.entrySet()) {
			try {
				URI prefixUri = URI.create(prefix.getKey()).normalize();
				URI scmUri = URI.create(scmPath).normalize();
				if (scmUri.toString().startsWith(prefixUri.toString())) {
					if (matching == null || prefix.getKey().length() > matching.getKey().length()) {
						matching = prefix;
					}
				}
			} catch (IllegalArgumentException e) {
				// in case mapping key is not a proper URL (i.e. CVS) compare strings
				if (scmPath.startsWith(prefix.getKey())) {
					if (matching == null || prefix.getKey().length() > matching.getKey().length()) {
						matching = prefix;
					}
				}
			}
		}
		return matching;
	}

	public static Map.Entry<String, String> getNamedSourceRepository(Map<String, String> repositories, String name) {
		for (Map.Entry<String, String> entry : repositories.entrySet()) {
			if (entry.getValue().equals(name)) {
				return entry;
			}
		}
		return null;
	}

	public static void setScmRepositoryMapping(TaskRepository repository, String scmPath, String value) {
		Map<String, String> mappings = getScmRepositoryMappings(repository);
		mappings.put(scmPath, value);
		setScmRepositoryMappings(repository, mappings);
	}
}
