package com.hiyoko.discord.bot.BCDice.dto;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

public class DicerollResult {
	private final String text;
	private final boolean secret;
	private final boolean rolled;
	private final boolean isError;
	private final String system;

	private final String UNSUPPORTED_DICEBOT = "unsupported dicebot";

	public DicerollResult(String text, String system, boolean secret, boolean rolled) {
		this.text = text;
		this.secret = secret;
		this.rolled = rolled;
		this.system = system;
		this.isError = false;
	}

	public DicerollResult(String text, String system, boolean secret, boolean rolled, boolean error) {
		this.text = text;
		this.secret = secret;
		this.rolled = rolled;
		this.system = system;
		this.isError = error;
	}

	public DicerollResult(String json, String usedSystem) {
		JsonObject result = Json.parse(json).asObject();
		this.rolled = result.getBoolean("ok", false);
		if(rolled) {
			this.system = usedSystem;
			this.text = result.getString("result", "[ERROR] コマンドが成功しているにも関わらずメッセージが取得できませんでした");
			this.secret = result.getBoolean("secret", false);
			this.isError = false;
		} else {
			this.system = "";
			this.secret = false;
			String text = result.getString("reason", "");
			
			if(text.equals(UNSUPPORTED_DICEBOT)) {
				this.isError = true;
				this.text = String.format("対応していないシステム ( `%s` ) を使っているようです。スペルが間違っている、または未対応のシステムかもしれません。対応しているシステムを `bcdice set システム名` で設定してください。ダイスボットの一覧を参照するには `bcdice list` をご利用ください", usedSystem) ;
			} else {
				this.isError = false;
				this.text = "";
			}
		}
	}

	public DicerollResult(String json) {
		JsonObject result = Json.parse(json).asObject();
		rolled = result.getBoolean("ok", false);
		if(rolled) {
			system = "DiceBot";
			text = result.getString("result", "[ERROR] コマンドが成功しているにも関わらずメッセージが取得できませんでした");
			secret = result.getBoolean("secret", false);
			this.isError = false;
		} else {
			system = "";
			secret = false;

			String text = result.getString("reason", "");
			if(text.equals(UNSUPPORTED_DICEBOT)) {
				this.isError = true;
				this.text = "対応していないシステムを使っているようです。スペルが間違っている、または未対応のシステムかもしれません。対応しているシステムを `bcdice set システム名` で設定してください。ダイスボットの一覧を参照するには `bcdice list` をご利用ください";
			} else {
				this.isError = false;
				this.text = "";
			}
		}
	}

	public String getText() {
		return text;
	}

	public String getSystem() {
		return system;
	}

	public boolean isSecret() {
		return secret;
	}

	public boolean isRolled() {
		return rolled;
	}

	public boolean isError() {
		return isError;
	}

	public String toString() {
		if(secret) {
			return system + ": [Secret Dice]";
		} else {
			return system + text;
		}
	}
}
