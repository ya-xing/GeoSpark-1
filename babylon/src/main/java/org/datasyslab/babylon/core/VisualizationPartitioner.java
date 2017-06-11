/**
 * FILE: VisualizationPartitioner.java
 * PATH: org.datasyslab.babylon.core.VisualizationPartitioner.java
 * Copyright (c) 2017 Arizona State University Data Systems Lab
 * All rights reserved.
 */
package org.datasyslab.babylon.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.spark.Partitioner;
import org.datasyslab.babylon.utils.Pixel;
import org.datasyslab.babylon.utils.RasterizationUtils;

import scala.Tuple2;

/**
 * The Class VisualizationPartitioner.
 */
public class VisualizationPartitioner extends Partitioner implements Serializable{

	public int resolutionX,resolutionY,partitionX,partitionY,partitionIntervalX,partitionIntervalY;
	
	
	/**
	 * Instantiates a new visualization partitioner.
	 *
	 * @param resolutionX the resolution X
	 * @param resolutionY the resolution Y
	 * @param partitionX the partition X
	 * @param partitionY the partition Y
	 * @throws Exception the exception
	 */
	public VisualizationPartitioner(int resolutionX, int resolutionY, int partitionX, int partitionY) throws Exception
	{
		this.resolutionX = resolutionX;
		this.resolutionY = resolutionY;
		this.partitionX = partitionX;
		this.partitionY = partitionY;
		if(this.resolutionX%partitionX!=0||this.resolutionY%partitionY!=0)
		{
			throw new Exception("[VisualizationPartitioner][Constructor] The given partition number fails to exactly divide the corresponding resolution axis.");
		}
		this.partitionIntervalX = this.resolutionX / this.partitionX;
		this.partitionIntervalY = this.resolutionY / this.partitionY;
	}
	
	/* (non-Javadoc)
	 * @see org.apache.spark.Partitioner#getPartition(java.lang.Object)
	 */
	@Override
	public int getPartition(Object key) {
		return ((Pixel) key).getCurrentPartitionId();
	}

	/* (non-Javadoc)
	 * @see org.apache.spark.Partitioner#numPartitions()
	 */
	@Override
	public int numPartitions() {
		return partitionX*partitionY;
	}

	/**
	 * Assign partition IDs to this pixel. One pixel may have more than one partition Id. This partitioning method will introduce
	 * duplicates to ensure that all neighby pixels (as well as their buffer) are in the same partition.
	 * @param pixelDoubleTuple2
	 * @return
	 */
	public List<Tuple2<Pixel, Double>> assignPartitionIDs(Tuple2<Pixel, Double> pixelDoubleTuple2, int photoFilterRadius)
	{
		ArrayList<Tuple2<Pixel, Double>> duplicatePixelList = new ArrayList<Tuple2<Pixel, Double>>();
		ArrayList<Integer> existingPartitionIds = new ArrayList<Integer>();
		try {
			// First, calculate the correct partition that the pixel belongs to
			int partitionId = RasterizationUtils.CalculatePartitionId(this.resolutionX,this.resolutionY,this.partitionX, this.partitionY, pixelDoubleTuple2._1.getX(), pixelDoubleTuple2._1.getY());
			Pixel newPixel = pixelDoubleTuple2._1();
			newPixel.setCurrentPartitionId(partitionId);
			newPixel.setDuplicate(false);
			duplicatePixelList.add(new Tuple2<Pixel, Double>(newPixel, pixelDoubleTuple2._2()));
			existingPartitionIds.add(partitionId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// Second, calculate the partitions that the pixel duplicates should go to
		for (int x = -photoFilterRadius; x <= photoFilterRadius; x++) {
			for (int y = -photoFilterRadius; y <= photoFilterRadius; y++) {
				int neighborPixelX = pixelDoubleTuple2._1().getX()+x;
				int neighborPixelY = pixelDoubleTuple2._1().getY()+y;
				try {
					int partitionId = RasterizationUtils.CalculatePartitionId(this.resolutionX,this.resolutionY,this.partitionX, this.partitionY, neighborPixelX, neighborPixelY);
					// This partition id is out of the image boundary
					if(partitionId<0) continue;
					if(!existingPartitionIds.contains(partitionId))
					{
						Pixel newPixel = pixelDoubleTuple2._1();
						newPixel.setCurrentPartitionId(partitionId);
						newPixel.setDuplicate(true);
						duplicatePixelList.add(new Tuple2<Pixel, Double>(newPixel, pixelDoubleTuple2._2()));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}
		return duplicatePixelList;

	}
	/**
	 * Assign partition IDs to this pixel. This partitioning method will not introduce
	 * duplicates.
	 * @param pixelDoubleTuple2
	 * @return
	 */
	public Tuple2<Pixel, Double> assignPartitionID(Tuple2<Pixel, Double> pixelDoubleTuple2)
	{
		int partitionId = RasterizationUtils.CalculatePartitionId(this.resolutionX,this.resolutionY,this.partitionX, this.partitionY, pixelDoubleTuple2._1.getX(), pixelDoubleTuple2._1.getY());
		Pixel newPixel = pixelDoubleTuple2._1();
		newPixel.setCurrentPartitionId(partitionId);
		newPixel.setDuplicate(false);
		return new Tuple2<Pixel,Double>(newPixel, pixelDoubleTuple2._2());
	}
}
