import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class Test {
    public static void main(String[] args) {
        byte b = 35;
        byte c = 70;
        byte d = 1;
        String jiwoo = "jiwoo";
        byte[] bcd ;
        byte[] test = new byte[3];
        test[0] = b;
        test[1] = c;
        test[2] = d;
        bcd = jiwoo.getBytes();

        System.out.println(bcd[0]);
        System.out.println(bcd[1]);
        System.out.println(bcd[2]);
        System.out.println(bcd[3]);
        System.out.println(bcd[4]);
        System.out.println(test[0]);

        int[] in = new int[3];
        in[0] = 1;
        System.out.println(bcd.length);
        }

}
