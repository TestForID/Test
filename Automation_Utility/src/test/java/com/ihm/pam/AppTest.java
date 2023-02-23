package com.ihm.pam;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    /**
     * Rigorous Test :-)
     */

    
    public static void main(String[] args)
    {
       String a = "abcdeafa";
       a.replaceAll("<br>", "#").replaceAll("\\|\\|", "**");
    }
}
