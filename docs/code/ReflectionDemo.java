// Java program  to demonstrate the use of Introspection by Reflection
// The Class metaobject will be retrieved starting from a className
// From the Class metaobject we retrieve information such as the superclass and the declared methods
// The details (method name, parameyter types, return type) are shown for a method
// having the maximum number of parameters  

import java.lang.reflect.Method;

public class ReflectionDemo {
    public static void main(String[] args) {

        String className = "java.lang.String";

        Class<?> c;

        try {
            c = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        System.out.println("I got the Class object of class "+className);

        Class<?> sc = c.getSuperclass();
        System.out.println("The superclass of " + className + " is " + sc.getName());

        Method[] methods = c.getDeclaredMethods();
        System.out.println("The class " + className + " has " + methods.length + " declared methods");

        int maxargs=-1;
        Method maxmethod=null;
        for (Method m : methods) {
            Class[] pts = m.getParameterTypes();
            if (pts.length>maxargs) {
                maxmethod=m;
                maxargs=pts.length;
            }
        }
        if (maxmethod!=null) {
            System.out.println("The method with the maximum number of arguments is "+maxmethod.getName());
            Class[] pts = maxmethod.getParameterTypes();
            System.out.println("Number of parameters for this method is  "+pts.length);
            int i=0;
            for (Class<?> pt:pts) {
                i++;
                System.out.print("Type of parameter "+i+" is: ");
                if (pt.isArray()) {
                    System.out.println("array of "+pt.getComponentType().getName());
                }
                else System.out.println(pt.getName());
            }
            System.out.println("Methods return type is: "+maxmethod.getReturnType().getName());
        }

    }
}
