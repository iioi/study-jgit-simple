package com.study.test1;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.util.FileUtils;

public class Demo1 {

	private static String username = "iioi";
	private static String password = "tel3482550";
	
	public static void main(String[] args) {
		UsernamePasswordCredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(username, password);
		try {
			FileUtils.delete(new File("repo_tmp/test4"), FileUtils.RECURSIVE);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		CloneCommand command = Git.cloneRepository().setDirectory(new File("repo_tmp/test4"));
		command.setCredentialsProvider(credentialsProvider);
		command.setURI("https://github.com/iioi/jenkins-groovy-simple.git");
		StringWriter outlog = new StringWriter();
		ProgressMonitor progressMonitor = new MyTextProgressMonitor(outlog);
		command.setProgressMonitor(progressMonitor);
		try {
			@SuppressWarnings("unused")
			Git git = command.call();
		} catch (GitAPIException e) {
			e.printStackTrace();
		}
		System.out.println(outlog);
	}
}
