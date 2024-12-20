package commandManager;

import commandLogic.CommandDescription;
import commandLogic.commandReceiverLogic.ReceiverManager;
import commandLogic.commandReceiverLogic.enums.ReceiverType;
import commandLogic.commandReceiverLogic.handlers.ArgumentReceiverHandler;
import commandLogic.commandReceiverLogic.handlers.ArgumentReceiverHandlerO;
import commandLogic.commandReceiverLogic.handlers.ArgumentReceiverHandlerP;
import commandLogic.commandReceiverLogic.handlers.NonArgReceiversHandler;

import commandManager.externalRecievers.*;
import exceptions.*;
import mods.ModeManager;
import mods.nonuser.OrganizationNonUserBuild;
import mods.nonuser.ProductNonUserBuild;
import mods.user.OrganizationUserInputBuilding;
import mods.user.ProductUserInputBuilding;
import products.Organization;
import products.Product;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Scanner;

import static commandManager.CommandMode.UserMode;
import static commandManager.CommandMode.NonUserMode;

//Основной класс, отвечающий за выполнение команд.
public class CommandExecutor {
    private static final Logger logger = LogManager.getLogger("CommandExecutor");


    private final ArrayList<CommandDescription> commands;
    private final Scanner scannerInput;
    private final CommandMode mode;
    private final ReceiverManager manager;



    public CommandExecutor(ArrayList<CommandDescription> commands, InputStream input, CommandMode mode) throws CommandsNotLoadedException {
        if (commands == null) throw new CommandsNotLoadedException();

        this.commands = commands;
        this.scannerInput = new Scanner(input);
        this.mode = mode;
        manager = new ReceiverManager();

        manager.registerHandler(ReceiverType.NoArgs, new NonArgReceiversHandler());
        manager.registerHandler(ReceiverType.ArgumentRouteP, new ArgumentReceiverHandlerP<>(Product.class));
        manager.registerHandler(ReceiverType.ArgumentRouteO, new ArgumentReceiverHandlerO<>(Organization.class));



        manager.registerReceiver(ReceiverType.NoArgs, new NonArgumentReceiver());
        manager.registerReceiver(ReceiverType.NoArgs, new ExecuteScriptReceiver());
        manager.registerReceiver(ReceiverType.NoArgs, new ExitReceiver());

        ModeManager<Product> modeManager = null;
        ModeManager<Organization> modeManagerO = null;
        switch (mode) {
            case UserMode -> {modeManager = new ProductUserInputBuilding();
            modeManagerO = new OrganizationUserInputBuilding();
            }
            case NonUserMode -> {modeManager = new ProductNonUserBuild(scannerInput);
                modeManagerO = new OrganizationNonUserBuild(scannerInput);}
        }
        manager.registerReceiver(ReceiverType.ArgumentRouteP, new ArgumentProductCommandReceiver(modeManager));
        manager.registerReceiver(ReceiverType.ArgumentRouteO, new ArgumentOrgCommandReceiver(modeManagerO));
    }


    public void startExecuting() {
        while (scannerInput.hasNext()) {
            String line = scannerInput.nextLine().trim();
            if (line.isEmpty()) continue;
            try {
                try {
                    String[] lineArgs = line.split("\\s+");
                    logger.info(lineArgs[0]);
                    CommandDescription description = Optional.ofNullable(commands).orElseThrow(CommandsNotLoadedException::new).stream().filter(x -> x.getName().equals(lineArgs[0])).findAny().orElseThrow(() -> new UnknownCommandException("Указанная команда не была обнаружена"));
                    description.getReceiver().callReceivers(manager, description, lineArgs);
                } catch (IllegalArgumentException | NullPointerException e) {
                    logger.warn("Выполнение команды пропущено из-за неправильных предоставленных аргументов! (" + e.getMessage() + ")");
                    throw new CommandInterruptedException(e);
                } catch (BuildObjectException | UnknownCommandException e) {
                    logger.error(e.getMessage());
                    throw new CommandInterruptedException(e);
                } catch (WrongAmountOfArgumentsException e) {
                    logger.error("Wrong amount of arguments! " + e.getMessage());
                    throw new CommandInterruptedException(e);
                } catch (Exception e) {
                    logger.error("В командном менеджере произошла непредвиденная ошибка! " + e.getMessage());
                    throw new CommandInterruptedException(e);
                }
            } catch (CommandInterruptedException ex) {
                if (mode.equals(UserMode))
                    logger.info("Выполнение команды было прервано. Вы можете продолжать работу. Программа возвращена в безопасное состояние.");
                else
                    logger.info("Команда была пропущена... Обработчик продолжает работу");
            }
        }
    }
}