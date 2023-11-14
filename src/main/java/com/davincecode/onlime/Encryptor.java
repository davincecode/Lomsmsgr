package com.davincecode.onlime;

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

        String password = "monkey123";
        String hashedPas = "cc25c0f861a83f5efadc6e1ba9d1269e";

        Scanner scanner = new Scanner(System.in);

        System.out.println("Hey! Plz input your Password: \n");

        String userInput = scanner.nextLine();

        if(encryptor.encryptString(userInput).equals(hashedPas)){
            System.out.println("Correct! You are in!");
        } else{
            System.out.println("Wrong!!");
        }

    }
}