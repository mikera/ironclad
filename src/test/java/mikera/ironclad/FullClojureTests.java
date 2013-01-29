package mikera.ironclad;

import mikera.cljunit.ClojureTest;

public class FullClojureTests extends ClojureTest {
  
	@Override
	public String filter() {
		return "ic.test";
	}
}
