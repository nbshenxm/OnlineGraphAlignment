package utils;

import java.util.regex.Pattern;


/**
 * Remove non-deterministic and instance specific information in each event's source and destination entities.
 */
public class EntityGeneralizer {

    // ToDo: make the Generalizer configurable.
    public static String filePathGeneralizer(String path) {
        path = path.toLowerCase();
//        path = removeFileName(path);
        path = removeTempPath(path);
        path = removeUserNameInFilePath(path);
        path = removeHashValuesInFilePath(path);
        return path;
    }

    public static String exeFilePathGeneralizer(String path) {
        path = path.toLowerCase();
//        path = removeFileName(path);
        path = removeUserNameInFilePath(path);
        path = removeHashValuesInFilePath(path);
        return path;
    }

    public static String argumentsGeneralizer(String cmdLineArguments) {
        cmdLineArguments = cmdLineArguments.toLowerCase();
        cmdLineArguments = removeValueInCmdLineArguments(cmdLineArguments);
        return cmdLineArguments;
    }

    private static String removeUserNameInFilePath(String path) {
        path = Pattern.compile("/home/[0-9a-z_-]+/").matcher(path).replaceFirst("/home/*/");
        return path;
    }

    private static String removeHashValuesInFilePath(String path) {
        path = Pattern.compile("[\\\\x][0-9a-f]{4,}").matcher(path).replaceAll("*");
        path = Pattern.compile("[0-9a-fopr-]{8,}").matcher(path).replaceAll("HASH");
        return path;
    }

    private static String removeTempPath(String path) {
        path = Pattern.compile("/tmp/[0-9a-z_-]+/").matcher(path).replaceAll("/tmp/*/");
        return path;
    }

    private static String removeFileName(String path) {
        int index = path.lastIndexOf("/");
        if (index == -1) return path;
        else path = path.substring(0, index);
        return path;
    }

    private static String removeValueInCmdLineArguments(String cmdLineArguments) {
//        String[] argumentsList = cmdLineArguments.toLowerCase().split("\\s+");
//        if (argumentsList.length <= 2)
//            return cmdLineArguments;
//
//        ArrayList<String> reservedArgumentsList = new ArrayList<String>();

        // interpreter + script
//        reservedArgumentsList.add(filePathGeneralizer(argumentsList[0]));
//        reservedArgumentsList.add(filePathGeneralizer(argumentsList[1]));
//        boolean isDashedArguments = (argumentsList[1].charAt(0) == '-' || argumentsList[2].charAt(0) == '-');
//
//        argumentsList = Arrays.copyOfRange(argumentsList, 2, argumentsList.length);
//        if (isDashedArguments) {
//            for (String argument : argumentsList) {
//                if (argument.charAt(0) == '-') {
//                    argument = argument.split("=")[0];
//                    reservedArgumentsList.add(argument);
//                }
//            }
//        }
//        else {
//            for (String argument : argumentsList) {
//                argument = filePathGeneralizer(argument);
//                reservedArgumentsList.add(argument);
//            }
//        }

//        return String.join(" ", reservedArgumentsList);
        return "";
    }
}