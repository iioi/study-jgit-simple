package com.study.achieve;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import com.study.test1.MyTextProgressMonitor;

public class GitHelper {

	// set集合保存当前操作中的本地git目录
	private static volatile Set<String> busyLocalRepo = new HashSet<>();
	// 线程池
	private static final ExecutorService exec = Executors.newCachedThreadPool();

	public static void loadLocalGitRepo(String gitUser, String gitPass, String gitUri, String gitBranch,
			StringWriter outlog) {
		// 通过传入git参数的uri和branch生成本地存放的路径
		String localGitDir = getLocalGitDir(gitUri, gitBranch);
		// 判断当前git仓库是否被占用，如果被占用则进行等待，等待完成后检查仓库，无问题直接返回
		if (busyLocalRepo.contains(localGitDir)) {
			outlog.write("INFO: the current git repository is busy, wait...\n");
			int i = 1; // 进行计时
			while (busyLocalRepo.contains(localGitDir)) {
				try {
					Thread.sleep(20000); // 以20s为单位进行等待
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				outlog.write("INFO: git has been wait for " + i * 20 + "s, still wait...\n");
			}
			outlog.write("INFO: git wait end. begin to check the local repository\n");
			if (checkLoalRepo(localGitDir)) {
				outlog.write("INFO: local git repository is ok\n");
				return;
			} else {
				outlog.write("WARN: the local git repository seems like having some problem\n");
				outlog.write("INFO: try to reload loacl git repository");
			}
		}
		//设置当前git目录为busy状态
		busyLocalRepo.add(localGitDir);
		// 判断当前localgit目录下是否有文件
		if (checkLoalRepo(localGitDir)) {
			// 如果有文件，进行pull操作
			Future<Boolean> future = exec.submit(() -> {
				return updateRepo(gitUser, gitPass, gitUri, gitBranch, outlog);
			});
			try {
				// 对pull操作设置超时时间为6分钟
				Boolean res = future.get(6, TimeUnit.MINUTES);
				if(!res) {
					//如果返回false，抛异常
					throw new Exception();
				}
			} catch (TimeoutException e) {
				// 超时后记录日志，抛出异常
				e.printStackTrace();
				outlog.write("ERROR: git pull cost more than 6 min, time out\n");
				outlog.write("\nTip: 请检查填写的git参数后重试");
			} catch (Exception e) {
				e.printStackTrace();
				outlog.write("ERROR: git pull fail, try to clone repository\n");
				outlog.write("\nTip: 请检查填写的git参数后重试");
			}finally {
				//解除busy状态
				busyLocalRepo.remove(localGitDir);
			}

		}else {
			//如果没有文件执行clone操作
			Future<Boolean> future = exec.submit(()->{
				return cloneRepo(gitUser, gitPass, gitUri, gitBranch, outlog);
			});
			try {
				// 对clone操作设置超时时间为6分钟
				Boolean res = future.get(6, TimeUnit.MINUTES);
				if(!res) {
					//如果返回false，抛异常
					throw new Exception();
				}
			} catch (TimeoutException e) {
				// 超时后记录日志，抛出异常
				e.printStackTrace();
				outlog.write("ERROR: git clone cost more than 6 min, time out\n");
				outlog.write("\nTip: 请检查填写的git参数后重试");
			} catch (Exception e) {
				e.printStackTrace();
				outlog.write("ERROR: git clone fail, try to clone repository\n");
				outlog.write("\nTip: 请检查填写的git参数后重试");
			}finally {
				//解除busy状态
				busyLocalRepo.remove(localGitDir);
			}
		}
	}

	private static boolean cloneRepo(String gitUser, String gitPass, String gitUri, String gitBranch,
			StringWriter outlog) {
		// 通过传入git参数的uri和branch生成本地存放的路径
		String localGitDir = getLocalGitDir(gitUri, gitBranch);
		UsernamePasswordCredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(gitUser,
				gitPass);
		CloneCommand command = Git.cloneRepository().setURI(gitUri).setBranch(gitBranch);
		command.setDirectory(new File(localGitDir));
		command.setCredentialsProvider(credentialsProvider);
		ProgressMonitor progressMonitor = new MyTextProgressMonitor(outlog);
		command.setProgressMonitor(progressMonitor);
		try {
			outlog.write("INFO: git begin to clone repository\n");
			command.call();
		} catch (GitAPIException e) {
			outlog.write("ERROR: git clone failed\n");
			outlog.write("\nTip: 请检查git仓库的uri、branch已经用户名和密码");
			e.printStackTrace();
			return false;
		}
		outlog.write("INFO: git clone success\n");
		return true;
	}

	private static boolean updateRepo(String gitUser, String gitPass, String gitUri, String gitBranch,
			StringWriter outlog) {
		// 通过传入git参数的uri和branch获取本地存放的路径
		String localGitDir = getLocalGitDir(gitUri, gitBranch);
		try {
			outlog.write("INFO: git begin to pull origin\n");
			Git git = Git.open(new File(localGitDir));
			git.pull();
		} catch (IOException e) {
			outlog.write("ERROR: git pull fail, try to clone repository\n");
			e.printStackTrace();
			return false;
		}
		outlog.write("INFO: git pull success\n");
		return true;
	}

	/**
	 * 检查本地git仓库的方法，其实目前只是判断了当前目录下有没有.git文件
	 * 
	 * @param path
	 */
	private static boolean checkLoalRepo(String path) {
		return new File(path, ".git").exists() && new File(path).list().length > 1;
	}

	public static String getLocalGitDir(String gitUri, String gitBranch) {
		gitUri = replaceSymbol(gitUri);
		gitBranch = replaceSymbol(gitBranch);
		return "repo_temp/" + gitUri + "/" + gitBranch;
	}

	private static String replaceSymbol(String str) {
		String res = str.replaceAll("[/:]", "");
		return res;
	}

	@Override
	protected void finalize() throws Throwable {
		// 关闭线程池
		exec.shutdown();
		super.finalize();
	}
}
