package com.hiyoko.discord.bot.BCDice.OriginalDiceBotClients;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hiyoko.discord.bot.BCDice.dto.OriginalDiceBot;

public class OriginalDiceBotClient {
	private final Client client;
	private final String dicebotDirectoryPath = "./originalDiceBots";
	private final File dicebotDirectory;
	private List<String> diceBotList;
	private final Logger logger = LoggerFactory.getLogger(OriginalDiceBotClient.class);
	public OriginalDiceBotClient() {
		client = ClientBuilder.newBuilder().build();
		dicebotDirectory = new File(dicebotDirectoryPath);
		if( ! dicebotDirectory.exists() ) {
			dicebotDirectory.mkdir();
		}
		diceBotList = getRawDiceBotList();
	}

	private String getAttachedFile(URL url) {
		Response response = client.target(url.toString()).request().get();
		return response.readEntity(String.class);
	}

	private boolean isExist(String targetName) {
		File targetFile = new File(String.format("%s/%s", dicebotDirectoryPath, targetName));
		return targetFile.exists();
	}

	private void writeFile(String title, String body) throws IOException {
		try (
				FileWriter fw = new FileWriter(String.format("%s/%s", dicebotDirectoryPath, title), true);
				PrintWriter pw = new PrintWriter(new BufferedWriter(fw));) {
			pw.print(body);
		} catch (IOException e) {
			throw new IOException(String.format("ダイスボット [%s] の作成に失敗しました", title), e);
		}
	}

	public void registerDiceBot(URL fileUrl, String fileName) throws IOException {
		String diceBotFile = getAttachedFile(fileUrl);
		writeFile(fileName, diceBotFile);
		diceBotList = getRawDiceBotList();
		logger.info(String.format("ダイスボット [%s] を登録しました", fileName));
	}

	public void unregisterDiceBot(String fileName) throws IOException {
		if(isExist(fileName)) {
			try {
				(new File(String.format("%s/%s", dicebotDirectoryPath, fileName))).delete();
			} catch (Exception e) {
				throw new IOException(String.format("ダイスボット[%s] の削除に失敗しました", fileName), e);
			}
			try {
				diceBotList = getRawDiceBotList();
			} catch (Exception e) {
				throw new IOException(String.format("ダイスボット[%s] の削除には成功しましたがダイスボット一覧の更新に失敗しました", fileName), e);
			}			
		} else {
			throw new IOException(String.format("ダイスボット[%s] は登録されていません", fileName));
		}
	}

	public OriginalDiceBot getDiceBot(String name) throws IOException {
		if(! diceBotList.contains(name)) {
			throw new IOException(String.format("ダイスボット [%s] が見つかりませんでした", name));
		}
		File file = new File(String.format("%s/%s", dicebotDirectoryPath, name));
		try (
				FileReader fr = new FileReader(file);
				BufferedReader br = new BufferedReader(fr);
				) {
			List<String> lines = new ArrayList<String>();
			String line;
			while((line = br.readLine()) != null) {
				lines.add(line.replaceAll("\\\\n", "\n"));
			}
			return new OriginalDiceBot(lines, name);
		} catch (IOException e) {
			throw new IOException(String.format("ダイスボット [%s] の読み込みに失敗しました", name));
		}
	}

	public List<String> getRawDiceBotList() {
		File[] list = dicebotDirectory.listFiles();
		List<String> fileList = new ArrayList<String>();
		for(File target : list) {
			fileList.add(target.getName());
		}
		return fileList;
	}

	public List<String> getDiceBotList() {
		return diceBotList;
	}
}
