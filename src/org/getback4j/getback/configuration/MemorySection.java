package org.getback4j.getback.configuration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.Validate;

/**
 * A type of {@link ConfigurationSection} that is stored in memory.
 */
public class MemorySection implements ConfigurationSection {

    protected final Map<String, Object> map = new LinkedHashMap<>();
    private final Configuration root;
    private final ConfigurationSection parent;
    private final String path;
    private final String fullPath;

    /**
     * Creates an empty MemorySection for use as a root {@link Configuration}
     * section.
     * <p>
     * Note that calling this without being yourself a {@link Configuration}
     * will throw an exception!
     *
     * @throws IllegalStateException Thrown if this is not a {@link
     *     Configuration} root.
     */
    protected MemorySection() {
        if (!(this instanceof Configuration)) {
            throw new IllegalStateException("Cannot construct a root MemorySection when not a Configuration");
        }

        this.path = "";
        this.fullPath = "";
        this.parent = null;
        this.root = (Configuration) this;
    }

    /**
     * Creates an empty MemorySection with the specified parent and path.
     *
     * @param parent Parent section that contains this own section.
     * @param path Path that you may access this section from via the root
     * {@link Configuration}.
     * @throws IllegalArgumentException Thrown is parent or path is null, or if
     * parent contains no root Configuration.
     */
    protected MemorySection(ConfigurationSection parent, String path) {
        Validate.notNull(parent, "Parent cannot be null");
        Validate.notNull(path, "Path cannot be null");

        this.path = path;
        this.parent = parent;
        this.root = parent.getRoot();

        Validate.notNull(root, "Path cannot be orphaned");

        this.fullPath = createPath(parent, path);
    }

    @Override
    public Set<String> getKeys(boolean deep) {
        Set<String> result = new LinkedHashSet<>();

        Configuration root2 = getRoot();
        if (root2 != null && root2.options().copyDefaults()) {
            ConfigurationSection defaults = getDefaultSection();

            if (defaults != null) {
                result.addAll(defaults.getKeys(deep));
            }
        }

        mapChildrenKeys(result, this, deep);

        return result;
    }

    @Override
    public Map<String, Object> getValues(boolean deep) {
        Map<String, Object> result = new LinkedHashMap<>();

        Configuration root2 = getRoot();
        if (root2 != null && root2.options().copyDefaults()) {
            ConfigurationSection defaults = getDefaultSection();

            if (defaults != null) {
                result.putAll(defaults.getValues(deep));
            }
        }

        mapChildrenValues(result, this, deep);

        return result;
    }

    @Override
    public boolean contains(String path) {
        return get(path) != null;
    }

    @Override
    public boolean isSet(String path) {
        Configuration root2 = getRoot();
        if (root2 == null) {
            return false;
        }
        if (root2.options().copyDefaults()) {
            return contains(path);
        }
        return get(path, null) != null;
    }

    @Override
    public String getCurrentPath() {
        return fullPath;
    }

    @Override
    public String getName() {
        return path;
    }

    @Override
    public Configuration getRoot() {
        return root;
    }

    @Override
    public ConfigurationSection getParent() {
        return parent;
    }

    @Override
    public void addDefault(String path, Object value) {
        Validate.notNull(path, "Path cannot be null");

        Configuration root2 = getRoot();
        if (root2 == null) {
            throw new IllegalStateException("Cannot add default without root");
        }
        if (root2 == this) {
            throw new UnsupportedOperationException("Unsupported addDefault(String, Object) implementation");
        }
        root2.addDefault(createPath(this, path), value);
    }

    @Override
    public ConfigurationSection getDefaultSection() {
        Configuration root2 = getRoot();
        Configuration defaults = root2 == null ? null : root2.getDefaults();

        if (defaults != null) {
            if (defaults.isConfigurationSection(getCurrentPath())) {
                return defaults.getConfigurationSection(getCurrentPath());
            }
        }

        return null;
    }

    @Override
    public void set(String path, Object value) {
        Validate.notEmpty(path, "Cannot set to an empty path");

        Configuration root2 = getRoot();
        if (root2 == null) {
            throw new IllegalStateException("Cannot use section without a root");
        }

        final char separator = root2.options().pathSeparator();
        // i1 is the leading (higher) index
        // i2 is the trailing (lower) index
        int i1 = -1, i2;
        ConfigurationSection section = this;
        while ((i1 = path.indexOf(separator, i2 = i1 + 1)) != -1) {
            String node = path.substring(i2, i1);
            ConfigurationSection subSection = section.getConfigurationSection(node);
            if (subSection == null) {
                section = section.createSection(node);
            } else {
                section = subSection;
            }
        }

        String key = path.substring(i2);
        if (section == this) {
            if (value == null) {
                map.remove(key);
            } else {
                map.put(key, value);
            }
        } else {
            section.set(key, value);
        }
    }

    @Override
    public Object get(String path) {
        return get(path, getDefault(path));
    }

    @Override
    public Object get(String path, Object def) {
        Validate.notNull(path, "Path cannot be null");

        if (path.length() == 0) {
            return this;
        }

        Configuration root2 = getRoot();
        if (root2 == null) {
            throw new IllegalStateException("Cannot access section without a root");
        }

        final char separator = root2.options().pathSeparator();
        // i1 is the leading (higher) index
        // i2 is the trailing (lower) index
        int i1 = -1, i2;
        ConfigurationSection section = this;
        while ((i1 = path.indexOf(separator, i2 = i1 + 1)) != -1) {
            section = section.getConfigurationSection(path.substring(i2, i1));
            if (section == null) {
                return def;
            }
        }

        String key = path.substring(i2);
        if (section == this) {
            Object result = map.get(key);
            return (result == null) ? def : result;
        }
        return section.get(key, def);
    }

    @Override
    public ConfigurationSection createSection(String path) {
        Validate.notEmpty(path, "Cannot create section at empty path");
        Configuration root2 = getRoot();
        if (root2 == null) {
            throw new IllegalStateException("Cannot create section without a root");
        }

        final char separator = root2.options().pathSeparator();
        // i1 is the leading (higher) index
        // i2 is the trailing (lower) index
        int i1 = -1, i2;
        ConfigurationSection section = this;
        while ((i1 = path.indexOf(separator, i2 = i1 + 1)) != -1) {
            String node = path.substring(i2, i1);
            ConfigurationSection subSection = section.getConfigurationSection(node);
            if (subSection == null) {
                section = section.createSection(node);
            } else {
                section = subSection;
            }
        }

        String key = path.substring(i2);
        if (section == this) {
            ConfigurationSection result = new MemorySection(this, key);
            map.put(key, result);
            return result;
        }
        return section.createSection(key);
    }

    @Override
    public ConfigurationSection createSection(String path, Map<?, ?> map) {
        ConfigurationSection section = createSection(path);

        map.entrySet().forEach((entry) -> {
            if (entry.getValue() instanceof Map) {
                section.createSection(entry.getKey().toString(), (Map<?, ?>) entry.getValue());
            } else {
                section.set(entry.getKey().toString(), entry.getValue());
            }
        });

        return section;
    }

    // Primitives
    @Override
    public String getString(String path) {
        Object def = getDefault(path);
        return getString(path, def != null ? def.toString() : null);
    }

    @Override
    public String getString(String path, String def) {
        Object val = get(path, def);
        return (val != null) ? val.toString() : def;
    }

    @Override
    public boolean isString(String path) {
        Object val = get(path);
        return val instanceof String;
    }

    @Override
    public int getInt(String path) {
        Object def = getDefault(path);
        return getInt(path, (def instanceof Number) ? toInt(def) : 0);
    }

    @Override
    public int getInt(String path, int def) {
        Object val = get(path, def);
        return (val instanceof Number) ? toInt(val) : def;
    }

    @Override
    public boolean isInt(String path) {
        Object val = get(path);
        return val instanceof Integer;
    }

    @Override
    public boolean getBoolean(String path) {
        Object def = getDefault(path);
        return getBoolean(path, (def instanceof Boolean) ? (Boolean) def : false);
    }

    @Override
    public boolean getBoolean(String path, boolean def) {
        Object val = get(path, def);
        return (val instanceof Boolean) ? (Boolean) val : def;
    }

    @Override
    public boolean isBoolean(String path) {
        Object val = get(path);
        return val instanceof Boolean;
    }

    @Override
    public double getDouble(String path) {
        Object def = getDefault(path);
        return getDouble(path, (def instanceof Number) ? toDouble(def) : 0);
    }

    @Override
    public double getDouble(String path, double def) {
        Object val = get(path, def);
        return (val instanceof Number) ? toDouble(val) : def;
    }

    @Override
    public boolean isDouble(String path) {
        Object val = get(path);
        return val instanceof Double;
    }

    @Override
    public long getLong(String path) {
        Object def = getDefault(path);
        return getLong(path, (def instanceof Number) ? toLong(def) : 0);
    }

    @Override
    public long getLong(String path, long def) {
        Object val = get(path, def);
        return (val instanceof Number) ? toLong(val) : def;
    }

    @Override
    public boolean isLong(String path) {
        Object val = get(path);
        return val instanceof Long;
    }

    // Java
    @Override
    public List<?> getList(String path) {
        Object def = getDefault(path);
        return getList(path, (def instanceof List) ? (List<?>) def : null);
    }

    @Override
    public List<?> getList(String path, List<?> def) {
        Object val = get(path, def);
        return (List<?>) ((val instanceof List) ? val : def);
    }

    @Override
    public boolean isList(String path) {
        Object val = get(path);
        return val instanceof List;
    }

    @Override
    public List<String> getStringList(String path) {
        List<?> list = getList(path);

        if (list == null) {
            return new ArrayList<>(0);
        }

        List<String> result = new ArrayList<>();

        list.stream().filter((object) -> ((object instanceof String) || (isPrimitiveWrapper(object)))).forEachOrdered((object) -> {
            result.add(String.valueOf(object));
        });

        return result;
    }

    @Override
    public List<Integer> getIntegerList(String path) {
        List<?> list = getList(path);

        if (list == null) {
            return new ArrayList<>(0);
        }

        List<Integer> result = new ArrayList<>();

        list.forEach((object) -> {
            if (object instanceof Integer) {
                result.add((Integer) object);
            } else if (object instanceof String) {
                try {
                    result.add(Integer.valueOf((String) object));
                } catch (NumberFormatException ex) {
                }
            } else if (object instanceof Character) {
                result.add((int) ((Character) object));
            } else if (object instanceof Number) {
                result.add(((Number) object).intValue());
            }
        });

        return result;
    }

    @Override
    public List<Boolean> getBooleanList(String path) {
        List<?> list = getList(path);

        if (list == null) {
            return new ArrayList<>(0);
        }

        List<Boolean> result = new ArrayList<>();

        list.forEach((object) -> {
            if (object instanceof Boolean) {
                result.add((Boolean) object);
            } else if (object instanceof String) {
                if (Boolean.TRUE.toString().equals(object)) {
                    result.add(true);
                } else if (Boolean.FALSE.toString().equals(object)) {
                    result.add(false);
                }
            }
        });

        return result;
    }

    @Override
    public List<Double> getDoubleList(String path) {
        List<?> list = getList(path);

        if (list == null) {
            return new ArrayList<>(0);
        }

        List<Double> result = new ArrayList<>();

        list.forEach((object) -> {
            if (object instanceof Double) {
                result.add((Double) object);
            } else if (object instanceof String) {
                try {
                    result.add(Double.valueOf((String) object));
                } catch (NumberFormatException ex) {
                }
            } else if (object instanceof Character) {
                result.add((double) ((Character) object));
            } else if (object instanceof Number) {
                result.add(((Number) object).doubleValue());
            }
        });

        return result;
    }

    @Override
    public List<Float> getFloatList(String path) {
        List<?> list = getList(path);

        if (list == null) {
            return new ArrayList<>(0);
        }

        List<Float> result = new ArrayList<>();

        list.forEach((object) -> {
            if (object instanceof Float) {
                result.add((Float) object);
            } else if (object instanceof String) {
                try {
                    result.add(Float.valueOf((String) object));
                } catch (NumberFormatException ex) {
                }
            } else if (object instanceof Character) {
                result.add((float) ((Character) object));
            } else if (object instanceof Number) {
                result.add(((Number) object).floatValue());
            }
        });

        return result;
    }

    @Override
    public List<Long> getLongList(String path) {
        List<?> list = getList(path);

        if (list == null) {
            return new ArrayList<>(0);
        }

        List<Long> result = new ArrayList<>();

        list.forEach((object) -> {
            if (object instanceof Long) {
                result.add((Long) object);
            } else if (object instanceof String) {
                try {
                    result.add(Long.valueOf((String) object));
                } catch (NumberFormatException ex) {
                }
            } else if (object instanceof Character) {
                result.add((long) ((Character) object));
            } else if (object instanceof Number) {
                result.add(((Number) object).longValue());
            }
        });

        return result;
    }

    @Override
    public List<Byte> getByteList(String path) {
        List<?> list = getList(path);

        if (list == null) {
            return new ArrayList<>(0);
        }

        List<Byte> result = new ArrayList<>();

        list.forEach((object) -> {
            if (object instanceof Byte) {
                result.add((Byte) object);
            } else if (object instanceof String) {
                try {
                    result.add(Byte.valueOf((String) object));
                } catch (NumberFormatException ex) {
                }
            } else if (object instanceof Character) {
                result.add((byte) ((Character) object).charValue());
            } else if (object instanceof Number) {
                result.add(((Number) object).byteValue());
            }
        });

        return result;
    }

    @Override
    public List<Character> getCharacterList(String path) {
        List<?> list = getList(path);

        if (list == null) {
            return new ArrayList<>(0);
        }

        List<Character> result = new ArrayList<>();

        list.forEach((object) -> {
            if (object instanceof Character) {
                result.add((Character) object);
            } else if (object instanceof String) {
                String str = (String) object;

                if (str.length() == 1) {
                    result.add(str.charAt(0));
                }
            } else if (object instanceof Number) {
                result.add((char) ((Number) object).intValue());
            }
        });

        return result;
    }

    @Override
    public List<Short> getShortList(String path) {
        List<?> list = getList(path);

        if (list == null) {
            return new ArrayList<>(0);
        }

        List<Short> result = new ArrayList<>();

        list.forEach((object) -> {
            if (object instanceof Short) {
                result.add((Short) object);
            } else if (object instanceof String) {
                try {
                    result.add(Short.valueOf((String) object));
                } catch (NumberFormatException ex) {
                }
            } else if (object instanceof Character) {
                result.add((short) ((Character) object).charValue());
            } else if (object instanceof Number) {
                result.add(((Number) object).shortValue());
            }
        });

        return result;
    }

    @Override
    public List<Map<?, ?>> getMapList(String path) {
        List<?> list = getList(path);
        List<Map<?, ?>> result = new ArrayList<>();

        if (list == null) {
            return result;
        }

        list.stream().filter((object) -> (object instanceof Map)).forEachOrdered((object) -> {
            result.add((Map<?, ?>) object);
        });

        return result;
    }

    @Override
    public ConfigurationSection getConfigurationSection(String path) {
        Object val = get(path, null);
        if (val != null) {
            return (val instanceof ConfigurationSection) ? (ConfigurationSection) val : null;
        }

        val = get(path, getDefault(path));
        return (val instanceof ConfigurationSection) ? createSection(path) : null;
    }

    @Override
    public boolean isConfigurationSection(String path) {
        Object val = get(path);
        return val instanceof ConfigurationSection;
    }

    protected boolean isPrimitiveWrapper(Object input) {
        return input instanceof Integer || input instanceof Boolean
                || input instanceof Character || input instanceof Byte
                || input instanceof Short || input instanceof Double
                || input instanceof Long || input instanceof Float;
    }

    protected Object getDefault(String path) {
        Validate.notNull(path, "Path cannot be null");

        Configuration root2 = getRoot();
        Configuration defaults = root2 == null ? null : root2.getDefaults();
        return (defaults == null) ? null : defaults.get(createPath(this, path));
    }

    protected void mapChildrenKeys(Set<String> output, ConfigurationSection section, boolean deep) {
        if (section instanceof MemorySection) {
            MemorySection sec = (MemorySection) section;

            sec.map.entrySet().stream().map((entry) -> {
                output.add(createPath(section, entry.getKey(), this));
                return entry;
            }).filter((entry) -> ((deep) && (entry.getValue() instanceof ConfigurationSection))).map((entry) -> (ConfigurationSection) entry.getValue()).forEachOrdered((subsection) -> {
                mapChildrenKeys(output, subsection, deep);
            });
        } else {
            Set<String> keys = section.getKeys(deep);

            keys.forEach((key) -> {
                output.add(createPath(section, key, this));
            });
        }
    }

    protected void mapChildrenValues(Map<String, Object> output, ConfigurationSection section, boolean deep) {
        if (section instanceof MemorySection) {
            MemorySection sec = (MemorySection) section;

            sec.map.entrySet().stream().map((entry) -> {
                output.put(createPath(section, entry.getKey(), this), entry.getValue());
                return entry;
            }).filter((entry) -> (entry.getValue() instanceof ConfigurationSection)).filter((entry) -> (deep)).forEachOrdered((entry) -> {
                mapChildrenValues(output, (ConfigurationSection) entry.getValue(), deep);
            });
        } else {
            Map<String, Object> values = section.getValues(deep);

            values.entrySet().forEach((entry) -> {
                output.put(createPath(section, entry.getKey(), this), entry.getValue());
            });
        }
    }

    /**
     * Creates a full path to the given {@link ConfigurationSection} from its
     * root {@link Configuration}.
     * <p>
     * You may use this method for any given {@link ConfigurationSection}, not
     * only {@link MemorySection}.
     *
     * @param section Section to create a path for.
     * @param key Name of the specified section.
     * @return Full path of the section from its root.
     */
    public static String createPath(ConfigurationSection section, String key) {
        return createPath(section, key, (section == null) ? null : section.getRoot());
    }

    /**
     * Creates a relative path to the given {@link ConfigurationSection} from
     * the given relative section.
     * <p>
     * You may use this method for any given {@link ConfigurationSection}, not
     * only {@link MemorySection}.
     *
     * @param section Section to create a path for.
     * @param key Name of the specified section.
     * @param relativeTo Section to create the path relative to.
     * @return Full path of the section from its root.
     */
    public static String createPath(ConfigurationSection section, String key, ConfigurationSection relativeTo) {
        Validate.notNull(section, "Cannot create path without a section");
        Configuration root = section.getRoot();
        if (root == null) {
            throw new IllegalStateException("Cannot create path without a root");
        }
        char separator = root.options().pathSeparator();

        StringBuilder builder = new StringBuilder();

        for (ConfigurationSection parent = section; (parent != null) && (parent != relativeTo); parent = parent.getParent()) {
            if (builder.length() > 0) {
                builder.insert(0, separator);
            }

            builder.insert(0, parent.getName());
        }

        if ((key != null) && (key.length() > 0)) {
            if (builder.length() > 0) {
                builder.append(separator);
            }

            builder.append(key);
        }

        return builder.toString();
    }

    @Override
    public String toString() {
        Configuration root2 = getRoot();
        return new StringBuilder()
                .append(getClass().getSimpleName())
                .append("[path='")
                .append(getCurrentPath())
                .append("', root='")
                .append(root2 == null ? null : root2.getClass().getSimpleName())
                .append("']")
                .toString();
    }

    public static int floor(double num) {
        final int floor = (int) num;
        return floor == num ? floor : floor - (int) (Double.doubleToRawLongBits(num) >>> 63);
    }

    public static int ceil(final double num) {
        final int floor = (int) num;
        return floor == num ? floor : floor + (int) (~Double.doubleToRawLongBits(num) >>> 63);
    }

    public static int round(double num) {
        return floor(num + 0.5d);
    }

    public static double square(double num) {
        return num * num;
    }

    public static int toInt(Object object) {
        if (object instanceof Number) {
            return ((Number) object).intValue();
        }

        try {
            return Integer.valueOf(object.toString());
        } catch (NumberFormatException | NullPointerException e) {
        }
        return 0;
    }

    public static float toFloat(Object object) {
        if (object instanceof Number) {
            return ((Number) object).floatValue();
        }

        try {
            return Float.valueOf(object.toString());
        } catch (NumberFormatException | NullPointerException e) {
        }
        return 0;
    }

    public static double toDouble(Object object) {
        if (object instanceof Number) {
            return ((Number) object).doubleValue();
        }

        try {
            return Double.valueOf(object.toString());
        } catch (NumberFormatException | NullPointerException e) {
        }
        return 0;
    }

    public static long toLong(Object object) {
        if (object instanceof Number) {
            return ((Number) object).longValue();
        }

        try {
            return Long.valueOf(object.toString());
        } catch (NumberFormatException | NullPointerException e) {
        }
        return 0;
    }

    public static short toShort(Object object) {
        if (object instanceof Number) {
            return ((Number) object).shortValue();
        }

        try {
            return Short.valueOf(object.toString());
        } catch (NumberFormatException | NullPointerException e) {
        }
        return 0;
    }

    public static byte toByte(Object object) {
        if (object instanceof Number) {
            return ((Number) object).byteValue();
        }

        try {
            return Byte.valueOf(object.toString());
        } catch (NumberFormatException | NullPointerException e) {
        }
        return 0;
    }
}
