package com.redeskyller.bukkit.solaryeconomy.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.junit.Before;
import org.junit.Test;

import com.redeskyller.bukkit.solaryeconomy.TestSupport;
import com.redeskyller.bukkit.solaryeconomy.command.FakeBukkit.Sender;
import com.redeskyller.bukkit.solaryeconomy.command.sub.AddCommand;
import com.redeskyller.bukkit.solaryeconomy.command.sub.CreateCommand;
import com.redeskyller.bukkit.solaryeconomy.command.sub.DeleteCommand;
import com.redeskyller.bukkit.solaryeconomy.command.sub.HelpCommand;
import com.redeskyller.bukkit.solaryeconomy.command.sub.MagnateCommand;
import com.redeskyller.bukkit.solaryeconomy.command.sub.PayCommand;
import com.redeskyller.bukkit.solaryeconomy.command.sub.ReloadCommand;
import com.redeskyller.bukkit.solaryeconomy.command.sub.RemoveCommand;
import com.redeskyller.bukkit.solaryeconomy.command.sub.SetCommand;
import com.redeskyller.bukkit.solaryeconomy.command.sub.ToggleCommand;
import com.redeskyller.bukkit.solaryeconomy.command.sub.TopCommand;
import com.redeskyller.bukkit.solaryeconomy.config.ConfigException;
import com.redeskyller.bukkit.solaryeconomy.config.SettingsHolder;
import com.redeskyller.bukkit.solaryeconomy.economy.EconomyServiceImpl;
import com.redeskyller.bukkit.solaryeconomy.economy.LeaderboardService;
import com.redeskyller.bukkit.solaryeconomy.format.AmountParser;
import com.redeskyller.bukkit.solaryeconomy.format.CurrencyFormatter;
import com.redeskyller.bukkit.solaryeconomy.hook.PrefixProvider;
import com.redeskyller.bukkit.solaryeconomy.message.Message;
import com.redeskyller.bukkit.solaryeconomy.message.Messages;

public class MoneyCommandTest {

	private static final String PAY = "solaryeconomy.commands.pay";

	private TestSupport.RecordingLogger logger;
	private EconomyServiceImpl economy;
	private LeaderboardService leaderboard;
	private MoneyCommand money;
	private Reloadable reloadTarget;
	private PrefixProvider prefixes;

	@Before
	public void setUp() {
		FakeBukkit.install();
		FakeBukkit.clearOnline();
		build(new HashMap<String, Object>());
	}

	private void build(Map<String, Object> overrides) {
		SettingsHolder settings = TestSupport.holder(overrides);
		this.logger = TestSupport.logger();
		this.economy = new EconomyServiceImpl(settings, this.logger);
		this.leaderboard = new LeaderboardService(this.economy, settings, this.logger);
		Messages messages = new Messages(TestSupport.bundledMessages(), this.logger);
		CurrencyFormatter formatter = new CurrencyFormatter(settings);
		AmountParser parser = new AmountParser(settings);
		this.reloadTarget = new Reloadable() {
			@Override
			public void reload() {
			}
		};
		this.prefixes = new PrefixProvider() {
			@Override
			public String getPrefix(String accountName) {
				return MoneyCommandTest.this.prefixForTests;
			}
		};

		this.money = new MoneyCommand(this.economy, formatter, messages, this.logger);
		this.money.register(new HelpCommand(messages, this.money))
				.register(new TopCommand(messages, this.leaderboard, formatter, settings, this.prefixes))
				.register(new CreateCommand(messages, this.economy, parser))
				.register(new DeleteCommand(messages, this.economy))
				.register(new AddCommand(messages, this.economy, parser, formatter, settings))
				.register(new RemoveCommand(messages, this.economy, parser, formatter, settings))
				.register(new SetCommand(messages, this.economy, parser, formatter, settings))
				.register(new PayCommand(messages, this.economy, parser, formatter, settings))
				.register(new ToggleCommand(messages, this.economy))
				.register(new ReloadCommand(messages, new Reloadable() {
					@Override
					public void reload() throws ConfigException {
						MoneyCommandTest.this.reloadTarget.reload();
					}
				}, this.logger))
				.register(new MagnateCommand(messages, this.leaderboard, formatter, settings, this.prefixes));
	}

	private String prefixForTests = "";

	private void run(Sender sender, String... args) {
		this.money.onCommand(sender.sender(), null, "money", args);
	}

	private void account(String name, String balance) {
		this.economy.createAccount(name, new BigDecimal(balance));
	}

	private void assertBalance(String expected, String name) {
		assertEquals(0, new BigDecimal(expected).compareTo(this.economy.getBalance(name)));
	}

	private static Map<String, Object> fullNumbers() {
		Map<String, Object> overrides = new HashMap<String, Object>();
		overrides.put("abbreviations.enable.messages", false);
		return overrides;
	}

	// ---- balance ----

	@Test
	public void aPlayerSeesTheirOwnBalance() {
		account("Alice", "1500");
		Sender alice = FakeBukkit.player("Alice");

		run(alice);

		assertEquals("Money: 1.5k", alice.last());
	}

	@Test
	public void aPlayerWithoutAnAccountGetsOneOnTheFirstCommand() {
		Sender alice = FakeBukkit.player("Alice");

		run(alice);

		assertTrue(this.economy.hasAccount("alice"));
		assertEquals("Money: 0", alice.last());
	}

	@Test
	public void theConsoleGetsAHintInsteadOfABalance() {
		Sender console = FakeBukkit.console();

		run(console);

		assertEquals("/money ajuda - ver os comandos do plugin.", console.last());
	}

	@Test
	public void viewingAnotherPlayerRequiresPermissionAndIgnoresCase() {
		account("Bob", "40");
		Sender nobody = FakeBukkit.player("Eve");
		Sender admin = FakeBukkit.player("Admin", Permissions.MONEY_OTHER);

		run(nobody, "bob");
		run(admin, "BOB");
		run(admin, "ghost");

		assertEquals("Você não tem permissão para isso.", nobody.last());
		assertEquals("Money de Bob: 40", admin.plain().get(0));
		assertEquals("Jogador não encontrado em nosso banco de dados.", admin.last());
	}

	@Test
	public void lookingUpYourselfByNameNeedsNoPermission() {
		account("Alice", "7");
		Sender alice = FakeBukkit.player("Alice");

		run(alice, "ALICE");

		assertEquals("Money: 7", alice.last());
	}

	// ---- pay ----

	@Test
	public void payMovesMoneyAndTellsBothPlayers() {
		build(fullNumbers());
		account("Alice", "1000");
		account("Bob", "0");
		Sender alice = FakeBukkit.player("Alice", PAY);
		Sender bob = FakeBukkit.player("Bob");

		run(alice, "pay", "bob", "250");

		assertBalance("750", "Alice");
		assertBalance("250", "Bob");
		assertEquals("Você enviou 250 coins para Bob", alice.last());
		assertEquals("Você acaba de receber 250 coins de Alice", bob.last());
	}

	@Test
	public void payRefusalsExplainWhyAndMoveNothing() {
		account("Alice", "100");
		account("Bob", "0");
		account("Carl", "0");
		this.economy.setReceivingDisabled("Carl", true);
		Sender alice = FakeBukkit.player("Alice", PAY);

		run(alice, "pay", "bob", "100.01");
		assertEquals("Você não tem money suficiente para isso.", alice.last());
		run(alice, "pay", "alice", "1");
		assertEquals("Você não pode enviar coins a si mesmo.", alice.last());
		run(alice, "pay", "ghost", "1");
		assertEquals("Jogador não encontrado em nosso banco de dados.", alice.last());
		run(alice, "pay", "carl", "1");
		assertEquals("Este jogador está com o recebimento de coins desativado.", alice.last());

		assertBalance("100", "Alice");
		assertBalance("0", "Bob");
		assertBalance("0", "Carl");
	}

	@Test
	public void payWithAnInvalidAmountAnswersInsteadOfCrashing() {
		// "pay x abc" used to end in a NullPointerException with abbreviations enabled (the default)
		account("Alice", "100");
		account("Bob", "0");
		Sender alice = FakeBukkit.player("Alice", PAY);

		for (String bad : new String[] { "abc", "0", "-5", "1e999999999", "5q", "0.5", "" }) {
			run(alice, "pay", "bob", bad);
			assertEquals(bad, "Valor incorreto, por favor insira um valor válido.", alice.last());
		}
		assertEquals(0, this.logger.count(Level.SEVERE));
		assertBalance("100", "Alice");
	}

	@Test
	public void payAcceptsAbbreviations() {
		account("Alice", "5000");
		account("Bob", "0");
		Sender alice = FakeBukkit.player("Alice", PAY);

		run(alice, "pay", "bob", "1,5k");

		assertBalance("3500", "Alice");
		assertBalance("1500", "Bob");
	}

	@Test
	public void payCannotOverflowTheReceiver() {
		Map<String, Object> overrides = fullNumbers();
		overrides.put("max_balance", "100");
		build(overrides);
		account("Alice", "100");
		account("Bob", "90");
		Sender alice = FakeBukkit.player("Alice", PAY);

		run(alice, "pay", "bob", "20");

		assertEquals("Essa operação ultrapassaria o saldo máximo permitido (100 coins).", alice.last());
		assertBalance("100", "Alice");
	}

	@Test
	public void payNeedsAPlayerAndAPermissionAndArguments() {
		account("Alice", "100");
		account("Bob", "0");
		Sender console = FakeBukkit.console();
		Sender alice = FakeBukkit.player("Alice"); // no permission

		run(console, "pay", "bob", "1");
		assertEquals("Apenas jogadores podem usar este comando.", console.last());

		run(alice, "pay", "bob", "1");
		assertEquals("Você não tem permissão para isso.", alice.last());

		Sender allowed = FakeBukkit.player("Alice", PAY);
		run(allowed, "pay", "bob");
		assertEquals("Use: /money pay [jogador] [valor]", allowed.last());
		assertBalance("100", "Alice");
	}

	// ---- admin commands ----

	@Test
	public void addRemoveAndSetWorkFromTheConsole() {
		build(fullNumbers());
		account("Bob", "100");
		Sender console = FakeBukkit.console();

		run(console, "add", "BOB", "50");
		assertEquals("Foi adicionado a conta de Bob a quantia de 50 coins", console.last());
		run(console, "remove", "bob", "30");
		assertEquals("Foi removido a quantia de 30 coins da conta de Bob", console.last());
		run(console, "set", "bob", "1.234,5");
		assertEquals("Valor incorreto, por favor insira um valor válido.", console.last());
		run(console, "set", "bob", "1234,5");
		assertEquals("Foi setado a quantia de 1.234,5 coins na conta de Bob", console.last());

		assertBalance("1234.5", "Bob");
	}

	@Test
	public void removeNeverTakesMoreThanTheBalance() {
		account("Bob", "10");
		Sender console = FakeBukkit.console();

		run(console, "remove", "bob", "11");

		assertEquals("O jogador não possui essa quantia.", console.last());
		assertBalance("10", "Bob");
	}

	@Test
	public void adminCommandsReportUnknownPlayersUsageAndMissingPermission() {
		Sender console = FakeBukkit.console();
		Sender player = FakeBukkit.player("Eve");

		run(console, "add", "ghost", "5");
		assertEquals("Jogador não encontrado em nosso banco de dados.", console.last());
		run(console, "add", "ghost");
		assertEquals("Use: /money add [jogador] [valor]", console.last());
		run(console, "set", "ghost", "0");
		assertEquals("Jogador não encontrado em nosso banco de dados.", console.last()); // zero is a valid set
		run(player, "add", "ghost", "5");
		assertEquals("Você não tem permissão para isso.", player.last());
	}

	@Test
	public void addRefusesZeroButSetAcceptsIt() {
		account("Bob", "10");
		Sender console = FakeBukkit.console();

		run(console, "add", "bob", "0");
		assertEquals("Valor incorreto, por favor insira um valor válido.", console.last());
		run(console, "set", "bob", "0");

		assertBalance("0", "Bob");
	}

	@Test
	public void createAndDeleteAccounts() {
		Sender console = FakeBukkit.console();

		run(console, "criar", "Shop", "100");
		assertEquals("Conta 'Shop' criada com sucesso!", console.last());
		run(console, "create", "SHOP", "5");
		assertEquals("Já existe uma conta com o nome 'SHOP'!", console.last());
		run(console, "criar", "bad§name", "5");
		assertEquals("Nome de conta inválido.", console.last());
		run(console, "deletar", "shop");
		assertEquals("Conta 'shop' deletada com sucesso!", console.last());
		run(console, "del", "shop");
		assertEquals("Conta 'shop' não encontrada em nosso banco de dados.", console.last());
		assertFalse(this.economy.hasAccount("Shop"));
	}

	// ---- toggle ----

	@Test
	public void toggleFlipsReceivingAndSaysWhichWay() {
		account("Alice", "10");
		Sender alice = FakeBukkit.player("Alice", "solaryeconomy.commands.toggle");

		run(alice, "toggle");
		assertEquals("Recebimento de coins: OFF", alice.last());
		assertTrue(this.economy.isReceivingDisabled("Alice"));

		run(alice, "toggle");
		assertEquals("Recebimento de coins: ON", alice.last());
		assertFalse(this.economy.isReceivingDisabled("Alice"));

		Sender console = FakeBukkit.console();
		run(console, "toggle");
		assertEquals("Apenas jogadores podem usar este comando.", console.last());
	}

	// ---- top / magnate ----

	@Test
	public void topListsTheRichestWithPrefixAndMagnateTag() {
		this.prefixForTests = "[VIP] ";
		account("Alice", "1500");
		account("Bob", "300");
		this.leaderboard.refresh();
		Sender console = FakeBukkit.console();

		run(console, "top");

		List<String> lines = console.plain();
		assertEquals("Top 10 Mais Ricos (Atualizado de 5 em 5 minutos)", lines.get(0));
		assertEquals("1. [$] [VIP] Alice: (1.5k)", lines.get(2));
		assertEquals("2. [VIP] Bob: (300)", lines.get(3));
	}

	@Test
	public void topAndMagnateSayWhenThereIsNobody() {
		Sender console = FakeBukkit.console();

		run(console, "top");
		run(console, "magnata");

		assertTrue(console.plain().contains("Não existe jogadores cadastrados ainda."));
		assertTrue(console.plain().contains("[$] Nenhum magnata ainda."));
	}

	@Test
	public void magnateShowsTheRichest() {
		account("Alice", "2500000");
		this.leaderboard.refresh();
		Sender console = FakeBukkit.console();

		run(console, "magnata");

		assertTrue(console.plain().contains("[$] Atual magnata do servidor: Alice com a quantia de 2.5m"));
	}

	// ---- help / dispatch / errors ----

	@Test
	public void helpListsOnlyWhatTheSenderMayUse() {
		Sender player = FakeBukkit.player("Eve", PAY);

		run(player, "ajuda");

		String text = player.plain().toString();
		assertTrue(text, text.contains("/money pay [jogador] [valor] - envia money a um jogador."));
		assertTrue(text, text.contains("/money - ver seu saldo atual."));
		assertTrue(text, text.contains("/money ajuda - ver os comandos do plugin."));
		assertFalse(text, text.contains("/money set"));
		assertFalse(text, text.contains("[jogador] - ver o saldo"));
	}

	@Test
	public void theConsoleSeesEveryCommandInTheHelp() {
		Sender console = FakeBukkit.console();

		run(console, "?");

		// two blank lines around: "/money", "/money [jogador]" and the 11 subcommands
		assertEquals(13, console.plain().size() - 2);
	}

	@Test
	public void aFailureInsideACommandIsLoggedWithTheCommandLineAndAnsweredPolitely() {
		this.money.register(new AbstractSubCommand(new Messages(TestSupport.bundledMessages(), this.logger), "explode",
				"explode", "", Message.HELP_HELP) {
			@Override
			public void execute(CommandContext context) {
				throw new IllegalStateException("bug");
			}
		});
		Sender console = FakeBukkit.console();

		run(console, "explode");

		assertEquals("Ocorreu um erro interno ao executar o comando. Avise a administração.", console.last());
		assertEquals(1, this.logger.count(Level.SEVERE));
		assertTrue(this.logger.contains(Level.SEVERE, "/money explode"));
	}

	@Test
	public void reloadReportsSuccessOrTheReasonItFailed() {
		Sender console = FakeBukkit.console();
		run(console, "reload");
		assertEquals("Arquivos de configurações recarregados com sucesso.", console.last());

		this.reloadTarget = new Reloadable() {
			@Override
			public void reload() throws ConfigException {
				throw new ConfigException("config.yml não é um YAML válido");
			}
		};
		run(console, "reload");

		assertEquals("Não foi possível recarregar (config.yml não é um YAML válido). Nada foi alterado.",
				console.last());
		assertEquals(1, this.logger.count(Level.WARNING));
	}

	@Test
	public void tabCompletionOffersOnlyPermittedSubcommandsMatchingThePrefix() {
		Sender player = FakeBukkit.player("Eve", PAY);

		List<String> all = this.money.onTabComplete(player.sender(), null, "money", new String[] { "" });
		List<String> p = this.money.onTabComplete(player.sender(), null, "money", new String[] { "P" });
		List<String> none = this.money.onTabComplete(player.sender(), null, "money", new String[] { "set", "x" });

		assertTrue(all.contains("pay"));
		assertTrue(all.contains("help"));
		assertFalse(all.contains("set"));
		assertEquals(Collections.singletonList("pay"), p);
		assertTrue(none.isEmpty());
	}

	@Test
	public void subcommandAliasesAndCaseAreHonored() {
		account("Bob", "5");
		Sender console = FakeBukkit.console();

		run(console, "GIVE", "bob", "5");
		run(console, "Take", "bob", "1");

		assertBalance("9", "Bob");
	}

	@Test
	public void everyHelpMessageResolvesToText() {
		Messages messages = new Messages(TestSupport.bundledMessages(), this.logger);
		for (Message message : Arrays.asList(Message.HELP_BALANCE, Message.HELP_PAY, Message.HELP_MAGNATA))
			assertFalse(messages.get(message).startsWith("<"));
	}
}
