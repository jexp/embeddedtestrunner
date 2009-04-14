package de.jexp.embeddedtestrunner;

import org.junit.internal.runners.CompositeRunner;
import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.internal.runners.InitializationError;
import org.junit.internal.runners.MethodValidator;
import org.junit.runner.notification.RunNotifier;

import java.util.*;
import java.net.URL;
import java.io.IOException;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * @author Michael Hunger
 * @since 13.04.2009
 */
public class EmbeddedRunner extends CompositeRunner {
    public EmbeddedRunner(final Class test) throws InitializationError {
        super(test.getName());
        final Collection<String> testClassNames = getPotentialTestClassNames(test);
        addChildren(test, testClassNames);
    }

    private Collection<String> getPotentialTestClassNames(final Class test) {
        try {
            final Collection<String> testClassNames = new ArrayList<String>();
            final String className = test.getSimpleName();
            final Enumeration<URL> pathResources = getPathResourcesForClass(test);
            while (pathResources.hasMoreElements()) {
                final URL url = pathResources.nextElement();
                final String[] files = getTestFileNames(className, url);
                testClassNames.addAll(createClassNames(test, files));
            }
            return testClassNames;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Collection<String> createClassNames(final Class test, final String[] files) {
        final Collection<String> testClassNames = new ArrayList<String>();
        final String packageName = test.getPackage().getName();
        for (final String file : files) {
            testClassNames.add(createClassName(packageName, file));
        }
        return testClassNames;
    }

    private Enumeration<URL> getPathResourcesForClass(final Class test) throws IOException {
        final String packageName = test.getPackage().getName();
        final ClassLoader classLoader = test.getClassLoader();
        final String packageDirectoryName = packageName.replace('.', File.separatorChar);
        return classLoader.getResources(packageDirectoryName);
    }

    private String createClassName(final String packageName, final String fileName) {
        return packageName + "." + fileName.substring(0, fileName.length() - ".class".length());
    }

    private String[] getTestFileNames(final String className, final URL url) {
        final String protocol = url.getProtocol();
        if ("file".equals(protocol)) {
            final File directory = new File(url.getPath());
            return directory.list(new FilenameFilter() {
                public boolean accept(final File file, final String fileName) {
                    return fileName.matches(className + "\\$\\d+\\w+\\.class");
                }
            });
        }
        // todo Jar Protocol
        return new String[0];
    }

    private void addChildren(final Class test, final Collection<String> testClassNames) throws InitializationError {
        final List<Throwable> errors = new ArrayList<Throwable>();
        final ClassLoader loader = test.getClassLoader();
        for (final String testClassName : testClassNames) {
            try {
                final Class testClass = loader.loadClass(testClassName);
                add(new InnerClassRunner(test, testClass));
            } catch (Exception e) {
                errors.add(e);
            }
        }
        if (!errors.isEmpty()) throw new InitializationError(errors);
    }

    static class InnerClassRunner extends JUnit4ClassRunner {
        private Constructor constructor;
        private Class<?> outerClass;

        public InnerClassRunner(final Class<?> outerClass, final Class testClass) throws NoSuchMethodException, InitializationError {
            super(testClass);
            this.outerClass = outerClass;
            constructor = testClass.getDeclaredConstructor(outerClass);
        }

        @Override
        protected Object createTest() throws Exception {
            return constructor.newInstance(outerClass.newInstance());
        }

        @Override
        protected void validate() throws InitializationError {
            final MethodValidator methodValidator = new MethodValidator(getTestClass());
            methodValidator.validateStaticMethods();
            methodValidator.validateInstanceMethods();
        }

        @Override
        protected void invokeTestMethod(final Method method, final RunNotifier notifier) {
            method.setAccessible(true);
            super.invokeTestMethod(method, notifier);
        }
    }
}
