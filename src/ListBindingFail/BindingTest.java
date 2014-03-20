package ListBindingFail;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.databinding.observable.set.WritableSet;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BindingTest {
	
	//WARN: these realms and their threads are never destroyed
	public static final DistinctRealm validationRealm = new DistinctRealm("Validation realm");
	public static final DistinctRealm targetRealm = new DistinctRealm("Target realm");
	public static final DistinctRealm modelRealm  = new DistinctRealm("Model realm");
	
	public static Throwable getError(final DistinctRealm realm) throws InterruptedException {
		return new RealmRead<Throwable>() {
			@Override
			protected Throwable compute() {
				return realm.getError();
			}
		}.syncExec(realm);
	}
	
 	@Before
 	public void resetError() throws InterruptedException {
		getError(validationRealm);
		getError(targetRealm);
		getError(modelRealm);
	}
	
 	static void rethrow (DistinctRealm realm ) throws InterruptedException {
 		Throwable e = getError(realm);
 		if (e != null) {
 			throw new RuntimeException("Error in " + realm, e);
 		}
 	}
 	
	@After
	public void rethrow() throws Throwable {
		rethrow(validationRealm);
		rethrow(targetRealm);
		rethrow(modelRealm);
	}
	
	@Test
	/** Tests list binding between different realms
	 *  Fails with org.eclipse.core.runtime.AssertionFailedException.AssertionFailedException
	 *  https://bugs.eclipse.org/bugs/show_bug.cgi?id=371791
	 * */
	public void testListBindingValidationRealm() throws Throwable {
		final DataBindingContext dbc = new DataBindingContext(validationRealm);
		final WritableList model = new WritableList(modelRealm, new ArrayList<String>(), String.class);
		final WritableList target = new WritableList(targetRealm, new ArrayList<String>(), String.class);
		new RealmRead<Binding>() {
			@Override
			protected Binding compute() {
				// BUG 371791: validationStatus is being written from target realm, error in org.eclipse.core.databinding.ListBinding.doUpdate()
				return dbc.bindList(target, model); 
			}
		}.syncExec(dbc.getValidationRealm());
		new RealmRead<Boolean>() {
			@Override
			protected Boolean compute() {
				return model.add("First element");
			}
		}.syncExec(modelRealm);
		new RealmRead<Boolean>() {
			@Override
			protected Boolean compute() {
				return target.add("Second element");
			}
		}.syncExec(targetRealm);
	}

	@Test
	/** Tests set binding between different realms
	 *  Fails with org.eclipse.core.runtime.AssertionFailedException.AssertionFailedException
	 *  https://bugs.eclipse.org/bugs/show_bug.cgi?id=371791
	 * */
	public void testSetBindingValidationRealm() throws InterruptedException {
		final DataBindingContext dbc = new DataBindingContext(validationRealm);
		final WritableSet model = new WritableSet(modelRealm, Collections.EMPTY_LIST, String.class);
		final WritableSet target = new WritableSet(targetRealm, Collections.EMPTY_LIST, String.class);

		new RealmRead<Binding>() {
			@Override
			protected Binding compute() {
				return dbc.bindSet(target, model); // BUG 371791: validationStatus is being written from target realm, error in org.eclipse.core.databinding.SetBinding.doUpdate()
			}
		}.syncExec(dbc.getValidationRealm());

		new RealmRead<Boolean>() {
			@Override
			protected Boolean compute() {
				return model.add("First element");
			}
		}.syncExec(model.getRealm());

		new RealmRead<Boolean>() {
			@Override
			protected Boolean compute() {
				return target.add("Second element"); //Fails
			}
		}.syncExec(target.getRealm());
	}

	
	static Object readValue(final IObservableValue value) throws InterruptedException {
		return new RealmRead<Object>() {
			@Override
			protected Object compute() {
				return value.getValue();
			}
		}.syncExec(value.getRealm());
	}
	
	@Test
	/** Tests value binding between different realms
	 * */
	public void testValueBindingValidationRealm() throws Throwable {
		final DataBindingContext dbc = new DataBindingContext(validationRealm);
		final WritableValue model = new WritableValue(modelRealm, "Initial value", String.class);
		final WritableValue target = new WritableValue(targetRealm, null, String.class);
		dbc.getValidationRealm().exec(new Runnable() {
			@Override
			public void run() {
				dbc.bindValue(target, model); // Binding successful, as expected
			}
		});

		rethrow(); // Synchronization between realms 
		rethrow();
		assertEquals("Initial value", readValue(target));

		model.getRealm().exec(new Runnable() {
			@Override
			public void run() {
				model.setValue("Second value");
			}
		});
		rethrow(); // Synchronization between realms 
		rethrow();
		assertEquals("Second value", readValue(target));
		
		target.getRealm().exec(new Runnable() {
			@Override
			public void run() {
				target.setValue("Third value");
			}
		});
		rethrow(); // Synchronization between realms 
		rethrow();
		assertEquals("Third value", readValue(model));
	}
	
	
}
