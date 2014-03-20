package ListBindingFail;

import org.eclipse.core.databinding.observable.Realm;

/**Synchronously invokes the computation code in another realm
 */
abstract class RealmRead<T>  {
	protected abstract T compute();
	@SuppressWarnings("unchecked")
	public T syncExec(Realm realm) throws InterruptedException {
		final Object[] result = new Object[]{null};
		final boolean[] ready = new boolean[]{false};
		final Throwable[] error = new Throwable[]{null};
		
		realm.exec(new Runnable() {
			public void run() {
				try {
					result[0] = compute();
				} catch (Throwable e) {
					error[0] = e;
					throw e;
				} finally {
					synchronized (ready) {
						ready[0] = true;
						ready.notifyAll();
					}
				}
			}
		});
		synchronized (ready) {
			while (!ready[0])
				ready.wait(100);
		}
		if (error[0] != null)
			throw new RuntimeException(error[0]);
		return (T) result[0];
	}
}