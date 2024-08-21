package com.slavogri;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello Dependency Parser !");

        new DirectoryTraverser ().traverseDirectory(System.getenv("root-dir"));
    }





}