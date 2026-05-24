package ru.mevgeniy.malinaticket;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import ru.mevgeniy.malinaticket.command.TicketCommand;
import ru.mevgeniy.malinaticket.config.CategoryRegistry;
import ru.mevgeniy.malinaticket.config.GuiConfig;
import ru.mevgeniy.malinaticket.config.MessageService;
import ru.mevgeniy.malinaticket.config.PluginSettings;
import ru.mevgeniy.malinaticket.gui.GuiService;
import ru.mevgeniy.malinaticket.listener.ChatInputListener;
import ru.mevgeniy.malinaticket.listener.GuiListener;
import ru.mevgeniy.malinaticket.listener.JoinListener;
import ru.mevgeniy.malinaticket.session.ChatInputService;
import ru.mevgeniy.malinaticket.storage.TicketStorage;

public final class MalinaTicketPlugin extends JavaPlugin {
    private PluginSettings settings;
    private MessageService messages;
    private CategoryRegistry categories;
    private GuiConfig guiConfig;
    private TicketStorage storage;
    private TicketService ticketService;
    private ChatInputService chatInputs;
    private GuiService guiService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResourceIfMissing("permissions.yml");

        settings = new PluginSettings(this);
        messages = new MessageService(this);
        categories = new CategoryRegistry(this);
        guiConfig = new GuiConfig(this);
        storage = new TicketStorage(this);
        chatInputs = new ChatInputService();

        reloadEverything();

        ticketService = new TicketService(storage, settings, messages, categories);
        guiService = new GuiService(storage, ticketService, settings, messages, guiConfig, categories, chatInputs);

        getServer().getPluginManager().registerEvents(new GuiListener(guiService), this);
        getServer().getPluginManager().registerEvents(new ChatInputListener(this, settings, messages, chatInputs, storage, ticketService), this);
        getServer().getPluginManager().registerEvents(new JoinListener(storage, messages), this);

        TicketCommand command = new TicketCommand(storage, ticketService, guiService, messages, chatInputs, this::reloadEverything);
        PluginCommand pluginCommand = getCommand("ticket");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        getLogger().info("MalinaTicket 26.5.8 включен. Хранилище: plugins/MalinaTicket/tickets/<ник>.yml");
    }

    private void saveResourceIfMissing(String resourceName) {
        if (!new java.io.File(getDataFolder(), resourceName).exists()) {
            saveResource(resourceName, false);
        }
    }

    public void reloadEverything() {
        settings.reload();
        messages.reload();
        categories.reload();
        guiConfig.reload();
        storage.loadAll();
        if (ticketService != null) {
            ticketService.reloadFormatter();
        }
    }
}
