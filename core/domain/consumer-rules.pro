# Order statuses are resolved by constant name from Firestore documents
# (OrderStatus.valueOf in the admin data source). R8 must not rename or strip
# the enum constants, otherwise every order silently falls back to PENDING in
# release builds and status-based filtering (e.g. failed payments) breaks.
-keepclassmembers enum com.mtdevelopment.core.model.OrderStatus { *; }
