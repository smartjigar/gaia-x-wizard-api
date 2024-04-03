package eu.gaiax.wizard.api.utils;

public class TenantContext {
    private static final ThreadLocal<String> currentTenant = new InheritableThreadLocal<>();
    private static final ThreadLocal<Boolean> useMasterDb = new InheritableThreadLocal<>();


    public static String getCurrentTenant() {
        return currentTenant.get();
    }


    public static void setCurrentTenant(String tenant) {
        currentTenant.set(tenant);
    }


    public static Boolean getUseMasterDb() {
        Boolean b = useMasterDb.get();
        if (b == null) {
            useMasterDb.set(false);
            return false;
        } else {
            return b;
        }
    }

    public static void setUseMasterDb(boolean userUseMasterDb) {
        useMasterDb.set(userUseMasterDb);
    }

    public static void clear() {
        currentTenant.remove();
    }
}
