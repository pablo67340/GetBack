package org.getback4j.getback.runnable;

/**
 *
 * @author Bryce
 *
 * This is just a class of handy statics you can use at any time
 */
public final class Utilities {

    // ANSI CODES
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";
    // END ANSI CODES

    /**
     * Capitalizes the first letter of a word
     *
     * @param str The string to be converted
     *
     * @return String The string with the first letter capitalized
     */
    public static String capitailizeWord(String str) {
        StringBuilder s = new StringBuilder();

        // Declare a character of space 
        // To identify that the next character is the starting 
        // of a new word 
        char ch = ' ';
        for (int i = 0; i < str.length(); i++) {

            // If previous character is space and current 
            // character is not space then it shows that 
            // current letter is the starting of the word 
            if (ch == ' ' && str.charAt(i) != ' ') {
                s.append(Character.toUpperCase(str.charAt(i)));
            } else {
                s.append(str.charAt(i));
            }
            ch = str.charAt(i);
        }

        // Return the string with trimming 
        return s.toString().trim();
    }

}
