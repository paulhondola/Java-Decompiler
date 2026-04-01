// Java program  to demonstrate the use of Manipulation by Reflection:
// a new instance is created  by reflection
// a method is invoked by reflection

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class DynamicDemo {
    public static void main(String[] args) {

        String className = "java.lang.String";  // name of class that will be instantiated
        String methodName = "length"; // name of method that will be invoked

        Class<?> c;

        try {
            c = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        System.out.println("I have now the Class object of class "+c.getName());

        Class<?>[] stringArgsClass = new Class[]{String.class};
        Constructor<?> ctor = null;
        try {
            ctor = c.getConstructor(stringArgsClass); // got the constructor that takes one String argument
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        System.out.println("I have now the constructor with one String argument for class "+c.getName());

        Object[] stringArgs = new Object[]{new String("abc")};
        Object something = null;
        try {
            something = ctor.newInstance(stringArgs); // instantiates a new object using the constructor with string "abc" as argument
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        System.out.println("I have instantiated the object something using reflection with the co nstructor object");
        System.out.println("The class of object something is "+something.getClass().getName()); // the new object (something) is an instance of  String
        System.out.println("Object something: "+something);

        Class<?>[] paramtypes = new Class[]{};
        Method lengthMethod = null;
        try {
            lengthMethod = c.getMethod(methodName, paramtypes); // find the  method of class String which has the name methodName="length" and no parameters
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        System.out.println("I have now the Method object reflecting method "+lengthMethod.getName() );

        Object[] argsM = new Object[]{};
        Object result = null;
        try {
            result = lengthMethod.invoke(something, argsM);  // invoke the method length on the object something
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Method "+lengthMethod.getName()+" was invoked on object something="+something);

        System.out.println("The type of the result is "+result.getClass().getName()); // the result is an integer
        System.out.println("The value of the  result is "+result);
    }
}
