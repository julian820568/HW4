import java.io.*;
import java.util.ArrayList;

public class IKDDhw4 {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		int num_file = 5;
		String query_str = new String( ""+args[0] );
		String str = null;
		int count;
		boolean [] appear_arr = new boolean [num_file];					//if input query appears in a file
		String [] name = new String [num_file];
		int size = 0;
		/*
		  *contains -> init_M
		  *       |
		  *      V
		  *    cuts     ->     M
		  */
		int [][] contains = new int [num_file][num_file];						//[j][i], show if i -> j
		//boolean dead_end;
		
		//---see if input query appears in input files---
		for( int i = 0; i < num_file; i++ ) {
			count = 0;
			FileInputStream fis = new FileInputStream("page" + (i+1) + ".txt");
			InputStreamReader isr = new InputStreamReader(fis,"MS950");
			BufferedReader buf = new BufferedReader(isr);
			while ( (str = buf.readLine()) != null) {
				if( str.contains(query_str) )
					count++;
				for( int j = 0; j < num_file; j++ )
					if( str.contains("http://page" + (j+1) + ".txt") )
						contains[j][i] = 1;
			}
			fis.close();
			//System.out.println("count:" + count);
			if( count != 0 )
				appear_arr[i] = true;
		}
		
		//---find elements of init_M---
		float [][] init_M = new float [num_file][num_file];
		for( int i = 0; i < num_file; i++ ) {
			float tmp = 0;
			for( int j = 0; j < num_file; j++ )
				tmp += contains[j][i];
			for( int j = 0; j < num_file; j++ )
				if( contains[j][i] == 1 )
					init_M[j][i] = 1/tmp;
		}
		/*for( int i = 0; i < num_file; i++ ) {
			for( int j = 0; j < num_file; j++ )
				System.out.print(init_M[i][j] + " ");
			System.out.println();
		}*/
		
		for( int i  = 0, j = 0; i < num_file; i++ )
			if( appear_arr[i] == true )
				name[j++] = new String("page" + (i+1) + ".txt");
		/*for( int j = 0; j < num_file; j++ )
			System.out.println(name[j]);*/
		
		//---cut dead end---
		int [][] cuts = new int [num_file][num_file];
		cuts = contains;
		boolean [] dead_end = new boolean [num_file];
		ArrayList<Integer> dead_end_order = new ArrayList<>();
		boolean cde = true;
		while( cde ) {
			cde = false;
			for( int i = 0; i < num_file; i++ ) {
				dead_end[i] = true;
				for( int j = 0; j < num_file; j++ )
					if( cuts[j][i] == 1 )
						dead_end[i] = false;
				if( dead_end[i] ) {
					if( !dead_end_order.contains(i) ) {
						dead_end_order.add(i);
						cde = true;
						for( int tmp = 0; tmp < num_file; tmp++ )
							cuts[i][tmp] = 0;
					}
				}
			}
		}
		
		//---fix size of M(trims files with "dead_end")---
		for( int i = 0; i < num_file; i++ )
			if( dead_end[i] == false  )
				size++;
		float [][] M = new float[size][size];
		//---find columns of M---
		int [] columns = new int [size];
		size = 0;
		for( int i = 0; i < num_file; i++ )
			if( dead_end[i] == false )
				columns[size++] = i;
		
		//---find elements of M---
		//---1. copy "cuts" to "M"
		for( int i = 0; i < M.length; i++ )
			for( int j = 0; j < M.length; j++ ) 
				M[j][i] = cuts[columns[j]][columns[i]];
		//---2. divide "column of M" by "number of 1s in a column"
		for( int i = 0; i < M.length; i++ ) {
			float tmp = 0;
			for( int j = 0; j < M.length; j++ )
				tmp += M[j][i];
			for( int j = 0; j < M.length; j++ )
				if( M[j][i] == 1 )
					M[j][i] = 1/tmp;
		}
		
		/*for( int i = 0; i < M.length; i++ )
			System.out.print(columns[i]);
		System.out.println();
		for( int i = 0; i < M.length; i++ ) {
			for( int j = 0; j < M.length; j++ )
				System.out.print(M[i][j] + " ");
			System.out.println();
		}*/
		
		//---find vN---
		float [] v0 = new float [M.length];
		for( int i = 0; i < M.length; i++ )
			v0[i] = 1/(float)M.length;
		float [] vN = markov( M, v0 );
		
		//---find extended vN---
		//---1.---
		float [] ex_vN = new float [num_file];	//extended vN
		for( int i = 0, j = 0; i < num_file; i++ ) {
			if( j < columns.length&& columns[j] == i )
				ex_vN[i] = vN[j++];
			else
				ex_vN[i] = 0;
		}
		//---2. use "dead_end_order" to get dead end page rank---
		for( int i = dead_end_order.size()-1; i >= 0; i-- ) {
			float newVn = 0;
			for( int j = 0; j < num_file; j++ )
				newVn += ex_vN[j] * init_M[dead_end_order.get(i)][j];
			ex_vN[dead_end_order.get(i)] = newVn;
		}
		
		for( int i = 0; i < num_file; i++ )
			System.out.print(ex_vN[i] + " ");
		System.out.println();
		
		//---trim pages without input query
		size = 0;
		for( int i = 0; i < num_file; i++ )
			if( appear_arr[i] == true )
				size++;
		float [] vN_final = new float [size];
		//---copy "ex_vN" to "vN_final"---
		for( int i = 0, j = 0; i < num_file; i++ )
			if( appear_arr[i] == true )
				vN_final[j++] = ex_vN[i];
		
		Sort( vN_final, name );
		
		String [] headings= new String[] { "Rank", "Filename" };
		Object [][] data = new Object[100][2];
		for( int oj = 0; oj < vN.length; oj++ ) {
			data[oj][0] = oj+1;
			data[oj][1] = new String( name[vN.length-1-oj] );
		}
		
		javax.swing.JTable table=new javax.swing.JTable(data,headings);
		javax.swing.JFrame MyFrame=new javax.swing.JFrame("Table");
		MyFrame.setSize(500,200);
		MyFrame.setLocation(200,200);
		MyFrame.getContentPane().add(new javax.swing.JScrollPane(table));
		MyFrame.setVisible(true);
	}
	
	public static float[] markov( float[][] M, float [] v0 ) {
		int N = 10;			//---number of iterations---
		
        for ( int t = 0; t < N; t++ ) {
        	// Compute effect of next move on page ranks. 
        	float [] new_v0 = new float [M.length];
        	for( int j = 0; j < M.length; j++ ) {
        		//  New rank of page j is dot product of old ranks and column j of p[][].
        		for( int k = 0; k < M.length; k++ )
        			new_v0[j] += v0[k] * M[j][k];
        	}
        	// Update page ranks.
        	v0 = new_v0;
        }
		
        // print page ranks
       /* for ( int i = 0; i < M.length; i++ )
        	System.out.print( v0[i] + " " );  
        System.out.println();*/
        
        return v0;
	}
	
	public static void Sort( float[] array, String[] namearr ) {
		for( int i = array.length - 1; i > 0; --i )
			for( int j = 0; j < i; ++j )
				if( array[j] > array[j + 1] ) {
					Swap( array, j, j + 1 );
					Swap( namearr, j, j + 1 );
                }
    }
 
	private static void Swap( float[] array, int indexA, int indexB) {
		float tmp = array[indexA];
		array[indexA] = array[indexB];
		array[indexB] = tmp;
	}
	
	private static void Swap( String[] array, int indexA, int indexB) {
		String tmp = array[indexA];
		array[indexA] = array[indexB];
		array[indexB] = tmp;
	}
}
