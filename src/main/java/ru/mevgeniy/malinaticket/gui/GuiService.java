package ru.mevgeniy.malinaticket.gui;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.mevgeniy.malinaticket.TicketService;
import ru.mevgeniy.malinaticket.config.CategoryRegistry;
import ru.mevgeniy.malinaticket.config.GuiConfig;
import ru.mevgeniy.malinaticket.config.GuiItemConfig;
import ru.mevgeniy.malinaticket.config.MessageService;
import ru.mevgeniy.malinaticket.config.PluginSettings;
import ru.mevgeniy.malinaticket.model.Ticket;
import ru.mevgeniy.malinaticket.model.TicketStatus;
import ru.mevgeniy.malinaticket.session.ChatInputService;
import ru.mevgeniy.malinaticket.storage.TicketStorage;
import ru.mevgeniy.malinaticket.util.ItemFactory;

public final class GuiService {
    final TicketStorage storage;
    final TicketService ticketService;
    final PluginSettings settings;
    final MessageService messages;
    final GuiConfig guiConfig;
    final CategoryRegistry categories;
    final ChatInputService chatInputs;
    final ItemFactory items;
    final GuiTextFormatter text;

    private final PlayerTicketMenu playerMenu;
    private final StaffTicketMenu staffMenu;
    private final TicketListMenu ticketListMenu;
    private final TicketViewMenu ticketViewMenu;
    private final CloseReasonMenu closeReasonMenu;

    public GuiService(
            TicketStorage storage,
            TicketService ticketService,
            PluginSettings settings,
            MessageService messages,
            GuiConfig guiConfig,
            CategoryRegistry categories,
            ChatInputService chatInputs
    ) {
        this.storage = storage;
        this.ticketService = ticketService;
        this.settings = settings;
        this.messages = messages;
        this.guiConfig = guiConfig;
        this.categories = categories;
        this.chatInputs = chatInputs;
        this.items = new ItemFactory(messages);
        this.text = new GuiTextFormatter(settings);
        this.playerMenu = new PlayerTicketMenu(this);
        this.staffMenu = new StaffTicketMenu(this);
        this.ticketListMenu = new TicketListMenu(this);
        this.ticketViewMenu = new TicketViewMenu(this);
        this.closeReasonMenu = new CloseReasonMenu(this);
    }

    public void showPlayerHome(Player player) {
        playerMenu.openHome(player);
    }

    public void showTicketCreation(Player player, String selectedCategoryId, int selectedPriority) {
        playerMenu.openCreateTicket(player, selectedCategoryId, selectedPriority);
    }

    public void showPlayerTickets(Player player, TicketStatus status, int page) {
        ticketListMenu.openOwnTickets(player, status, page);
    }

    public void showStaffDashboard(Player player) {
        staffMenu.openDashboard(player);
    }

    public void showStaffTickets(Player player, TicketStatus status, String categoryId, boolean assignedOnly, int page) {
        ticketListMenu.openStaffTickets(player, status, categoryId, assignedOnly, page);
    }

    public void showTicketDetails(Player player, Ticket ticket) {
        ticketViewMenu.openTicketDetails(player, ticket);
    }

    public void showCloseReasonPicker(Player player, Ticket ticket) {
        closeReasonMenu.openReasonPicker(player, ticket);
    }

    public void playClick(Player player) {
        if (settings.guiClickSound()) {
            player.playSound(player.getLocation(), settings.clickSound(), SoundCategory.MASTER, 0.35F, 1.25F);
        }
    }

    Inventory createInventory(MenuHolder holder, String menuName, Map<String, String> placeholders) {
        int size = guiConfig.size(menuName, switch (menuName) {
            case "create", "list", "view" -> 54;
            case "staff" -> 45;
            case "main" -> 36;
            case "close_reasons" -> 27;
            default -> 27;
        });
        Component title = messages.componentFromText(guiConfig.title(menuName, "<#D94F70>MalinaTicket"), placeholders);
        Inventory inventory = Bukkit.createInventory(holder, size, title);
        holder.inventory(inventory);
        return inventory;
    }

    void fillBackground(Inventory inventory) {
        GuiItemConfig filler = guiConfig.item("filler", -1, "GRAY_STAINED_GLASS_PANE", " ", List.of());
        ItemStack item = items.build(filler, Map.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, item);
        }
    }

    void placeClickableItem(Inventory inventory, MenuHolder holder, GuiItemConfig item, Map<String, String> placeholders, Consumer<InventoryClickEvent> action) {
        if (item.slot() < 0 || item.slot() >= inventory.getSize()) {
            return;
        }
        inventory.setItem(item.slot(), items.build(item, placeholders));
        holder.action(item.slot(), action);
    }

    GuiItemConfig itemAt(GuiItemConfig item, int slot) {
        return new GuiItemConfig(slot, item.material(), item.name(), item.lore(), item.amount(), item.glow(), item.customModelData());
    }
}
