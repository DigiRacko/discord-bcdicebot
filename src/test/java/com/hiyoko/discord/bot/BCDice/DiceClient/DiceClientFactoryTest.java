package com.hiyoko.discord.bot.BCDice.DiceClient;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class DiceClientFactoryTest{

	@Test
	public void testGetDiceClient() {
		assertTrue(DiceClientFactory.getDiceClient("https://bcdice.herokuapp.com").toString().startsWith("[BCDiceClient]"));
		assertTrue(DiceClientFactory.getDiceClient("test").toString().startsWith("[DiceClientMock]"));
	}

}
