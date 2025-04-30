package pcd.ass02.foopack;

import pcd.ass02.example.A;

public class B {
    public static void printFiles(){
        A.files.forEach(System.out::println);
    }
}
