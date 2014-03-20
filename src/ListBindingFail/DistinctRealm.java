package ListBindingFail;

import org.eclipse.core.databinding.observable.Realm;

public class DistinctRealm extends Realm {
	private ThreadLocal<Boolean> isCurrent = new ThreadLocal<Boolean>() {
		@Override
		protected Boolean initialValue() {
			return false;
		}
	};
	
	private final String name;

	public DistinctRealm(String name) {
		super();
		this.name = name;
	}

	private Throwable error;
	
	@Override
	public boolean isCurrent() {
		return isCurrent.get(); 
	}

	@Override
	protected synchronized void syncExec(Runnable runnable) {
		assert !isCurrent();
		isCurrent.set(true);
		try {
			runnable.run();
		} catch(Throwable ex) {
			if (error == null)
				error = ex;
		} finally {
			isCurrent.set(false);
		}
	}

	/** Gets a first error that happened in this realm after the last get */
	public synchronized Throwable getError() {
		if (!isCurrent())
			throw new IllegalAccessError();
		Throwable tmp = error;
		error = null;
		return tmp;
	}

	@Override
	public String toString() {
		return name;
	}
	
}
