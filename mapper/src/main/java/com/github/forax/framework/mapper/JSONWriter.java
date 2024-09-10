package com.github.forax.framework.mapper;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class JSONWriter {

  private interface Generator {
    String generate(JSONWriter writer, Object bean);
  }

  public static final ClassValue<List<Generator>> BEAN_INFO = new ClassValue<>() {
    @Override
    protected List<Generator> computeValue(Class<?> type) {
      var beanInfo = Utils.beanInfo(type);
      var properties = beanInfo.getPropertyDescriptors();
      return Arrays.stream(properties)
              .filter(property -> !property.getName().equals("class"))
              .<Generator>map(property -> {
                var getter = property.getReadMethod();
                var annotation = getter.getAnnotation(JSONProperty.class);
                var name = annotation == null ? property.getName() : annotation.value();
                return (writer, bean) -> "\"" + name + "\": " + writer.getJson(bean, getter);
              })
              .toList();
    }
  };

  public String toJSON(Object o) {
    return switch (o) {
      case null -> "null";
      case Boolean _, Integer _, Double _ -> o.toString();
      case String s -> "\"" + s + "\"";
      default ->{
        var generators = BEAN_INFO.get(o.getClass());
        yield generators.stream()
                .map(generator -> generator.generate(this, o))
                .collect(Collectors.joining(", ", "{","}"));
      }
    };
  }

  private String getJson(Object o, Method getter) {
    return toJSON(Utils.invokeMethod(o, getter));
  }

}
