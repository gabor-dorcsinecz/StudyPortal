package org.tresto.utils.security;

/**
 *
 * @author hu3b1188
 */
public class PasswordCheckResult {

    public int score = 0;
    public String verdict = "none";
    public String reasoning = "";
    public boolean isSatisfied = false;

    @Override public String toString() {
        return "isSatisfied: " + isSatisfied + "   score: " + score + "\r\nverdict: " + verdict + "\r\nreasoning: " + reasoning; 
    }
}
