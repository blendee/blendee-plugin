package org.blendee.plugin;

import org.eclipse.jdt.ui.text.java.AbstractProposalSorter;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

public class ProposalSorter extends AbstractProposalSorter {

	@Override
	public int compare(ICompletionProposal p1, ICompletionProposal p2) {
		return 0;
	}
}
