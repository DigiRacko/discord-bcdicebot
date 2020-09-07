package com.hiyoko.discord.bot.BCDice;

import com.hiyoko.discord.bot.BCDice.dto.DicerollResult;
import com.hiyoko.discord.bot.BCDice.dto.OriginalDiceBot;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({RandomStringUtils.class})
public class BCDiceCLITest{

	private BCDiceCLI cli;
	private static final String BCDICE_PASSWORD = "BCDICE_PASSWORD";
	private static final String PASSWORD = "mypassword";

	@Before
	public void setUp() {
		// Systemsのmockはうまく働かないため, ランダム生成のパスワードを固定することで対応.
		PowerMockito.mockStatic(RandomStringUtils.class);
		PowerMockito.when(RandomStringUtils.randomAscii(Mockito.anyInt())).thenReturn(PASSWORD);
		cli = new BCDiceCLI("mock");
	}

	@Test
	public void testIsRoll() {
		assertFalse(cli.isRoll("bcdice hiyoko"));
		assertFalse(cli.isRoll("BCDice hiyoko"));
		assertFalse(cli.isRoll("BCDice"));
		assertTrue(cli.isRoll("bcdiceだよ"));
		assertTrue(cli.isRoll("2d6"));
		assertTrue(cli.isRoll("koneko"));
	}

	@Test
	public void testRoll() {
		try {
			String system = "kindness";
			cli.inputs("bcdice set " + system, "", "neko");
			DicerollResult dr = cli.roll("2d6", "neko");
			assertEquals(dr.getSystem(), system);
		} catch (IOException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testInputStringHelp() {
		assertEquals(cli.inputs("bcdice help", "", "channel").get(0) , BCDiceCLI.HELP);
		assertEquals(cli.inputs("bcdice", "", "channel").get(0), BCDiceCLI.HELP);
		assertEquals(cli.inputs("bcdice nonsense", "", "channel").get(0), BCDiceCLI.HELP);
		String[] diceBotList = cli.inputs("bcdice list", "dummy", "dummy").get(0).split("\n");
		assertTrue(cli.inputs("bcdice help nonsense", "", "channel").get(0).indexOf("is not found") > -1);
		assertTrue(cli.inputs("bcdice help " + diceBotList[1], "", "channel").get(0).indexOf("is not found") == -1);
		assertTrue(cli.inputs("bcdice help " + diceBotList[1] + "\ndayodayo", "", "channel").get(0).indexOf("is not found") == -1);
	}
	
	public void testInputStringSet() {
		String[] diceBotList = cli.inputs("bcdice list", "dummy", "dummy").get(0).split("\n");
		assertTrue(cli.inputs("bcdice set", "", "channel").get(0).indexOf("ERROR") > -1);
		assertTrue(cli.inputs("bcdice set " + diceBotList[1], "", "channel").get(0).indexOf("ERROR") == -1);
		assertTrue(cli.inputs("bcdice set " + diceBotList[1] + " nonsense", "", "channel").get(0).indexOf("ERROR") == -1);
		assertTrue(cli.inputs("bcdice set " + diceBotList[1] + "\nhiyohiyo", "", "channel").get(0).indexOf("ERROR") == -1);
		assertTrue(cli.inputs("bcdice set Hiyoko", "", "hiyohitsu").get(0).contains("Hiyoko"));
		assertTrue(cli.inputs("bcdice set hitsuji & hiyoko", "", "hiyohitsu").get(0).contains("hitsuji & hiyoko"));
	}

	@Test
	public void testInputStringStack() {
		String[] list = {"hiyoko", "hiyoko hitsuji", "hiyoko\nhitsuji", "hiyoko hitsuji\nkoneko\nkoinu"};
		try {
			assertEquals(cli.inputs("bcdice save " + list[0], "hiyoko", "channel").get(0), "1");
			assertEquals(cli.inputs("bcdice save " + list[1], "hiyoko", "channel").get(0), "2");
			assertEquals(cli.inputs("bcdice save " + list[2], "hiyoko", "channel").get(0), "3");
			assertEquals(cli.inputs("bcdice save " + list[3], "hiyoko", "channel").get(0), "4");
			assertEquals(cli.inputs("bcdice save", "hiyoko", "channel").get(0), "5");
			
			assertEquals(cli.inputs("bcdice load 1", "hiyoko", "channel").get(0), list[0]);
			assertEquals(cli.inputs("bcdice load 2", "hiyoko", "channel").get(0), list[1]);
			assertEquals(cli.inputs("bcdice load 3", "hiyoko", "channel").get(0), list[2]);
			assertEquals(cli.inputs("bcdice load 4", "hiyoko", "channel").get(0), list[3]);
			assertEquals(cli.inputs("bcdice load 5", "hiyoko", "channel").get(0), "");
			assertTrue(cli.inputs("bcdice load 1", "", "channel").get(0).startsWith("Not found"));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testInputStringString() {
		assertEquals(cli.inputs("bcdice status", "", "channel").get(0), cli.inputs("bcdice status", "koneko", "channel").get(0));
	}

	@Test
	public void testMultiChannel() {
		String[] diceBotList = cli.inputs("bcdice list", "", "channel").get(0).split("\n");
		cli.inputs("bcdice set " + diceBotList[1], "hiyoko", "no_id");
		assertEquals(cli.inputs("bcdice status", "hiyoko", "general").get(0), cli.inputs("bcdice status", "hiyoko", "general").get(0));
		assertEquals(cli.inputs("bcdice status", "hiyoko", "general").get(0), cli.inputs("bcdice status", "hiyoko", "ungeneral").get(0));
		assertTrue(cli.inputs("bcdice status", "hiyoko", "ungeneral").get(0).contains(diceBotList[1]));
		cli.inputs("bcdice set " + diceBotList[2], "hiyoko", "ungeneral");
		assertTrue(cli.inputs("bcdice status", "hiyoko", "ungeneral").get(0).contains(diceBotList[2]));
		assertFalse(cli.inputs("bcdice status", "hiyoko", "general").get(0).contains(diceBotList[2]));
		assertTrue(cli.inputs("bcdice status", "hiyoko", "dummydummy").get(0).contains(diceBotList[1]));
	}

	@Test
	public void testNormalizeCommand() throws IOException {
		// From https://github.com/Shunshun94/discord-bcdicebot/pull/10#issuecomment-374023404
		String acctualText = cli.roll("2d6 <= 8 / ああああaaa[~'()&?!]", "nonChannel").getText();
		String expectedText = "2d6%3C%3D8%20%2F%20%E3%81%82%E3%81%82%E3%81%82%E3%81%82aaa%5B~%27%28%29%26%3F%21%5D";
		assertEquals(expectedText, acctualText);
		assertEquals("1d10%3C5", cli.roll("1d10 < 5", "nonChannel").getText());
		assertEquals("1d10%3E5", cli.roll("1d10 > 5", "nonChannel").getText());
		assertEquals("2d6aa%20a%3Cbb%20b%3Dc%20cc%3Edd%20d%3C%3D%3E%3D%3D%3C%3D%3Edd%20d", cli.roll("2d6aa a < bb b = c cc > dd d <= >=  =< => dd d", "nonChannel").getText());
	}

	@Test
	public void testAdmin() {
		assertTrue(cli.inputs("bcdice admin InvalidPassword help", "", "channel").get(0).contains("パスワードが違います"));
		assertEquals(cli.inputs("bcdice admin " + PASSWORD + " help", "", "channel").get(0), BCDiceCLI.HELP_ADMIN);
	}

	@Test
	public void testSupressionMode() throws IOException {
		String PREFIX = "/hiyoko";
		assertTrue(cli.inputs("bcdice admin " + PASSWORD + " suppressroll", "", "channel").get(0).contains("まずコマンドじゃないだろう"));
		assertTrue(cli.roll("サンプルダイスボット-夜食表", "no_channel").isRolled());
		assertFalse(cli.roll(PREFIX + " サンプルダイスボット-夜食表", "no_channel").isRolled());
		assertTrue(cli.roll("2d6", "channel").isRolled());
		assertFalse(cli.roll(PREFIX + " 2d6", "channel").isRolled());
		assertFalse(cli.roll("あああああ", "channel").isRolled());
		assertFalse(cli.roll(PREFIX + " あああああ", "channel").isRolled());

		assertTrue(cli.inputs("bcdice admin " + PASSWORD + " suppressroll disable", "", "channel").get(0).contains("すべてのコマンドがサーバに送信されます"));
		assertTrue(cli.roll("サンプルダイスボット-夜食表", "no_channel").isRolled());
		assertTrue(cli.roll(PREFIX + " サンプルダイスボット-夜食表", "no_channel").isRolled());
		assertTrue(cli.roll("2d6", "channel").isRolled());
		assertTrue(cli.roll(PREFIX + " 2d6", "channel").isRolled());
		assertTrue(cli.roll("あああああ", "channel").isRolled());
		assertTrue(cli.roll(PREFIX + " あああああ", "channel").isRolled());

		assertTrue(cli.inputs("bcdice admin " + PASSWORD + " suppressroll /hiyoko", "", "channel").get(0).contains("で始まるコマンドのみサーバに送信します "));
		assertFalse(cli.roll("サンプルダイスボット-夜食表", "no_channel").isRolled());
		assertTrue(cli.roll(PREFIX + "サンプルダイスボット-夜食表", "no_channel").isRolled());
		assertTrue(cli.roll("サンプルダイスボット-夜食表", "no_channel").getText().isEmpty());
		assertFalse(cli.roll(PREFIX + " サンプルダイスボット-夜食表", "no_channel").getText().isEmpty());
		assertFalse(cli.roll("2d6", "channel").isRolled());
		assertTrue(cli.roll(PREFIX + " 2d6", "channel").isRolled());
		assertFalse(cli.roll("あああああ", "channel").isRolled());
		assertTrue(cli.roll(PREFIX + " あああああ", "channel").isRolled());

		assertTrue(cli.inputs("bcdice admin " + PASSWORD + " suppressroll", "", "channel").get(0).contains("まずコマンドじゃないだろう"));
		assertTrue(cli.roll("サンプルダイスボット-夜食表", "no_channel").isRolled());
		assertFalse(cli.roll(PREFIX + " サンプルダイスボット-夜食表", "no_channel").isRolled());
		assertFalse(cli.roll("サンプルダイスボット-夜食表", "no_channel").getText().isEmpty());
		assertTrue(cli.roll(PREFIX + " サンプルダイスボット-夜食表", "no_channel").getText().isEmpty());
		assertTrue(cli.roll("2d6", "channel").isRolled());
		assertFalse(cli.roll(PREFIX + " 2d6", "channel").isRolled());
		assertFalse(cli.roll("あああああ", "channel").isRolled());
		assertFalse(cli.roll(PREFIX + " あああああ", "channel").isRolled());
		
		assertTrue(cli.inputs("bcdice admin " + PASSWORD + " suppressroll " + PREFIX, "", "channel").get(0).contains("で始まるコマンドのみサーバに送信します "));
		assertFalse(cli.roll("サンプルダイスボット-夜食表", "no_channel").isRolled());
		assertTrue(cli.roll(PREFIX + " サンプルダイスボット-夜食表", "no_channel").isRolled());
		assertTrue(cli.roll("サンプルダイスボット-夜食表", "no_channel").getText().isEmpty());
		assertFalse(cli.roll(PREFIX + " サンプルダイスボット-夜食表", "no_channel").getText().isEmpty());
		assertFalse(cli.roll("2d6", "channel").isRolled());
		assertTrue(cli.roll(PREFIX + " 2d6", "channel").isRolled());
		assertFalse(cli.roll("あああああ", "channel").isRolled());
		assertTrue(cli.roll(PREFIX + " あああああ", "channel").isRolled());
		
		assertTrue(cli.inputs("bcdice admin " + PASSWORD + " suppressroll disable", "", "channel").get(0).contains("すべてのコマンドがサーバに送信されます"));
		assertTrue(cli.roll("サンプルダイスボット-夜食表", "no_channel").isRolled());
		assertTrue(cli.roll(PREFIX + " サンプルダイスボット-夜食表", "no_channel").isRolled());
		assertFalse(cli.roll("サンプルダイスボット-夜食表", "no_channel").getText().isEmpty());
		assertTrue(cli.roll("2d6", "channel").isRolled());
		assertTrue(cli.roll(PREFIX + " 2d6", "channel").isRolled());
		assertTrue(cli.roll("あああああ", "channel").isRolled());
		assertTrue(cli.roll(PREFIX + " あああああ", "channel").isRolled());

		assertTrue(cli.inputs(PREFIX + "bcdice help サンプルダイスボット-夜食表", "", "channel").get(0).contains(OriginalDiceBot.NO_HELP_MESSAGE));
		assertTrue(cli.inputs(PREFIX + "bcdice help サンプルダイスボット-ラーメン表", "", "channel").get(0).contains("Which ramen noodle you should eat"));

		assertTrue(cli.inputs("bcdice admin " + PASSWORD + " suppressroll", "", "channel").get(0).contains("まずコマンドじゃないだろう"));
	}

	@Test
	public void testOriginalDiceBot() throws IOException {
		assertTrue(cli.roll("サンプルダイスボット-夜食表", "no_channel").isRolled());
	}

	@Test
	public void testMultiroll() throws Exception {
		assertEquals(cli.rolls("2d6", "no_channel").size(), 1);
		assertEquals(cli.rolls("3 2d6", "no_channel").size(), 3);
		assertEquals(cli.rolls("[パンダ,うさぎ,コアラ] 2d6", "no_channel").size(), 3);
		assertEquals(cli.rolls("3 サンプルダイスボット-夜食表", "no_channel").size(), 3);
		assertEquals(cli.rolls("20 サンプルダイスボット-夜食表", "no_channel").size(), 20);
		assertEquals(cli.rolls("21 なにもない", "no_channel").size(), 0);
		try {
			assertEquals(cli.rolls("21 サンプルダイスボット-夜食表", "no_channel").size(), 20);
			throw new Exception("Unexpected behavior [21 サンプルダイスボット-夜食表] must be rejected");
		} catch(IOException e) {
			// OK
		}

		String PREFIX = "/hiyoko";
		assertTrue(cli.inputs("bcdice admin " + PASSWORD + " suppressroll " + PREFIX, "", "channel").get(0).contains("で始まるコマンドのみサーバに送信します "));
		assertEquals(cli.rolls(PREFIX + " 2d6", "no_channel").size(), 1);
		assertEquals(cli.rolls(PREFIX + " 3 2d6", "no_channel").size(), 3);
		assertEquals(cli.rolls(PREFIX + " [パンダ,うさぎ,コアラ] 2d6", "no_channel").size(), 3);
		assertEquals(cli.rolls(PREFIX + " 3 サンプルダイスボット-夜食表", "no_channel").size(), 3);
		assertEquals(cli.rolls(PREFIX + " 20 サンプルダイスボット-夜食表", "no_channel").size(), 20);
		// 間にスペースなし
		assertEquals(cli.rolls(PREFIX + "2d6", "no_channel").size(), 1);
		assertEquals(cli.rolls(PREFIX + "3 2d6", "no_channel").size(), 3);
		assertEquals(cli.rolls(PREFIX + "[パンダ,うさぎ,コアラ] 2d6", "no_channel").size(), 3);
		assertEquals(cli.rolls(PREFIX + "3 サンプルダイスボット-夜食表", "no_channel").size(), 3);
		assertEquals(cli.rolls(PREFIX + "20 サンプルダイスボット-夜食表", "no_channel").size(), 20);

		assertTrue(cli.rolls(PREFIX + "2 2d6", "no_channel").get(0).getText().contains("2D6"));
		assertTrue(cli.rolls(PREFIX + "6 2d6", "no_channel").get(0).getText().contains("2D6"));
		assertTrue(cli.rolls(PREFIX + "1 1d12", "no_channel").get(0).getText().contains("1d12"));
	}
}
