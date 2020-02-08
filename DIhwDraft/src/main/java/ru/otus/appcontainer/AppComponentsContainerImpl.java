package ru.otus.appcontainer;

import ru.otus.appcontainer.api.AppComponent;
import ru.otus.appcontainer.api.AppComponentsContainer;
import ru.otus.appcontainer.api.AppComponentsContainerConfig;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class AppComponentsContainerImpl implements AppComponentsContainer {

    private static final Set<String> INVALID_APP_COMPONENT_NAMES = new HashSet<>(){{
        add("");
    }};

    private final Map<String, Class[]> nameAndParamMatching = new HashMap<>();
    private final Map<Class, String> classAndNameMatching = new HashMap<>();
    private final Set<String> appComponentNames = new HashSet<>();
    private final Map<String, Object> appComponentsByName = new HashMap<>();

    public AppComponentsContainerImpl(Class<?> initialConfigClass) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        processConfig(initialConfigClass);
    }

    private void processConfig(Class<?> configClass) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        checkConfigClass(configClass);

        Map<Integer, Set<String>> prepData = new HashMap<>();

        Method[] declaredMethods = configClass.getDeclaredMethods();
        for (Method declaredMethod : declaredMethods) {
            if (declaredMethod.isAnnotationPresent(AppComponent.class)){
                AppComponent annotation = declaredMethod.getAnnotation(AppComponent.class);
                String name = annotation.name();
                Integer order = annotation.order();

                checkAppComponentName(name);

                appComponentNames.add(name);
                nameAndParamMatching.put(name, declaredMethod.getParameterTypes());

                if (prepData.containsKey(order)){
                    prepData.get(order).add(name);
                } else {
                    prepData.put(order, new TreeSet<>(){{add(name);}});
                }
            }
        }

        Object config = configClass.getConstructor().newInstance();

        Set<Integer> keys = prepData.keySet();
        for (Integer key : keys) {
            Set<String> names = prepData.get(key);
            for (String name : names) {
                Class[] params = nameAndParamMatching.get(name);
                Method declaredMethod = configClass.getDeclaredMethod(name, params);
                Object[] objects = new Object[params.length];
                for(int i = 0; i < params.length; i++){
                    objects[i] = appComponentsByName.get(
                        classAndNameMatching.get(params[i])
                    );
                }

                Object invoke = declaredMethod.invoke(config, objects);
                appComponentsByName.put(name, invoke);
                classAndNameMatching.put(declaredMethod.getReturnType(), name);
            }
        }
    }

    private void checkConfigClass(Class<?> configClass) {
        if (!configClass.isAnnotationPresent(AppComponentsContainerConfig.class)) {
            throw new IllegalArgumentException(String.format("Given class is not config %s", configClass.getName()));
        }
    }

    private void checkAppComponentName(String appComponentName){
        if (INVALID_APP_COMPONENT_NAMES.contains(appComponentName)){
            throw new IllegalArgumentException(String.format("Invalid name %s", appComponentName));
        } else if (appComponentNames.contains(appComponentName)){
            throw new IllegalArgumentException(String.format("Duplicate name %s", appComponentName));
        }
    }

    @Override
    public <C> C getAppComponent(Class<C> componentClass) {
        return getAppComponent(classAndNameMatching.get(componentClass));
    }

    @Override
    public <C> C getAppComponent(String componentName) {
        return (C) appComponentsByName.get(componentName);
    }
}
