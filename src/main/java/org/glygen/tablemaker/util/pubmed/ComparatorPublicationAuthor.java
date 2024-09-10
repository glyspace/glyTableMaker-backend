package org.glygen.tablemaker.util.pubmed;

import java.util.Comparator;

public class ComparatorPublicationAuthor implements
		Comparator<DTOPublicationAuthor> 
{
	public int compare(DTOPublicationAuthor author1, DTOPublicationAuthor author2)
    {
        return author1.getOrder().compareTo(author2.getOrder());
    }
}