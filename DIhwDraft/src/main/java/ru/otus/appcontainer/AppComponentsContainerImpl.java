package ru.otus.appcontainer;

import ru.otus.appcontainer.api.AppComponent;
import ru.otus.appcontainer.api.AppComponentsContainer;
import ru.otus.appcontainer.api.AppComponentsContainerConfig;

import java.lang.reflect.Method;
import java.util.*;

public class AppComponentsContainerImpl implements AppComponentsContainer {

    private static final Set<Character> CHECKING_FILTER_DATA = new HashSet<>(){{
        add(' ');
        add('\t');
    }};

    private final Map<String, Class[]> nameAndParamMatching = new HashMap<>();

    private final Set<String> appComponentNames = new HashSet<>();
    private final Map<String, Object> appComponentsByName = new HashMap<>();
    private final Map<Class, Object> appComponentsByClass = new HashMap<>();

    public AppComponentsContainerImpl(Class<?> initialConfigClass) throws Exception {
        processConfig(initialConfigClass);
    }

    private void processConfig(Class<?> configClass) throws Exception {
        checkConfigClass(configClass);

        Map<Integer, Set<String>> preparedData = prepareMethodData(configClass);
        Object config = configClass.getConstructor().newInstance();

        for (Map.Entry<Integer, Set<String>> entity : preparedData.entrySet()){
            Set<String> names = entity.getValue();
            for (String name : names) {
                Class[] params = nameAndParamMatching.get(name);
                Method method = configClass.getDeclaredMethod(name, params);
                Object component = createComponent(config, method, params);

                Class<?> returnType = method.getReturnType();
                Class<?> componentClass = component.getClass();
                appComponentsByClass.put(returnType, component);
                if (!returnType.equals(componentClass)){
                    appComponentsByClass.put(componentClass, component);
                }

                appComponentsByName.put(name, component);
            }
        }
    }

    private Map<Integer, Set<String>> prepareMethodData(Class<?> configClass){

        Map<Integer, Set<String>> preparedData = new HashMap<>();
        Method[] declaredMethods = configClass.getDeclaredMethods();
        for (Method declaredMethod : declaredMethods) {
            if (declaredMethod.isAnnotationPresent(AppComponent.class)){
                AppComponent annotation = declaredMethod.getAnnotation(AppComponent.class);
                String name = annotation.name();
                Integer order = annotation.order();

                checkAppComponentName(name);

                appComponentNames.add(name);
                nameAndParamMatching.put(name, declaredMethod.getParameterTypes());

                if (preparedData.containsKey(order)){
                    preparedData.get(order).add(name);
                } else {
                    preparedData.put(order, new TreeSet<>(){{add(name);}});
                }
            }
        }

        return preparedData;
    }

    private Object createComponent(Object config, Method method, Class[] params) throws Exception {
        Object[] objects = new Object[params.length];
        for(int i = 0; i < params.length; i++){
            if (appComponentsByClass.containsKey(params[i])){
                objects[i] = appComponentsByClass.get(params[i]);
            } else {
                throw new Exception(String.format("The component %s doesn't exist", params[i]));
            }
        }

        return method.invoke(config, objects);
    }

    private void checkConfigClass(Class<?> configClass) {
        if (!configClass.isAnnotationPresent(AppComponentsContainerConfig.class)) {
            throw new IllegalArgumentException(String.format("Given class is not config %s", configClass.getName()));
        }
    }

    private void checkAppComponentName(String appComponentName){
        String name = new String(appComponentName);
        for (Character checkingFilterDatum : CHECKING_FILTER_DATA) {
            name = name.replace(checkingFilterDatum.toString(), "");
        }

        if (name.isEmpty()){
            throw new IllegalArgumentException(String.format("Invalid name '%s'", appComponentName));
        } else if (appComponentNames.contains(appComponentName)){
            throw new IllegalArgumentException(String.format("Duplicate name '%s'", appComponentName));
        }
    }

    @Override
    public <C> C getAppComponent(Class<C> componentClass) {
        return (C) appComponentsByClass.get(componentClass);
    }

    @Override
    public <C> C getAppComponent(String componentName) {
        return (C) appComponentsByName.get(componentName);
    }
}
