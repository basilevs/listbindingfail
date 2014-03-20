listbindingfail
===============

Junit test illustrating Eclipse platform error

https://bugs.eclipse.org/bugs/show_bug.cgi?id=371791

		final DataBindingContext dbc = new DataBindingContext(validationRealm);
		final WritableSet model = new WritableSet(modelRealm, Collections.EMPTY_LIST, String.class);
		final WritableSet target = new WritableSet(targetRealm, Collections.EMPTY_LIST, String.class);
                validationRealm.exec(new Runnable(){
			@Override
			public void run() {
				dbc.bindSet(target, model); // BUG 371791: validationStatus is being written from target realm, error in org.eclipse.core.databinding.SetBinding.doUpdate()
			}
		});
