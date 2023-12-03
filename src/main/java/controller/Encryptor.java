/*
 * Copyright (C) Vincent Ybanez 2023-Present
 * All Rights Reserved 2023
 */
package controller;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

public class Encryptor {

    public String encryptString(String input) throws NoSuchAlgorithmException {

        //MessageDigest works with MD2, MD5, SHA-1, SHA-224, SHA-256
        //SHA-384 and SHA-512
        MessageDigest md = MessageDigest.getInstance("MD5");

        byte[] messageDigest = md.digest(input.getBytes());

        BigInteger bigInt = new BigInteger(1,messageDigest);

        return bigInt.toString(16);
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        Encryptor encryptor = new Encryptor();

        String password = "davincecode";
        String hashedPas = "cc25c0f861a83f5efadc6e1ba9d1269e";

        Scanner scanner = new Scanner(System.in);

        System.out.println("Please enter your Password: \n");

        String userInput = scanner.nextLine();

        if(encryptor.encryptString(userInput).equals(hashedPas)){
            System.out.println("Password Accepted!");
        } else{
            System.out.println("Password Rejected!");
        }
    }
}