package com.minecolonies.achievements;

import com.minecolonies.items.ModItems;

/**
 * Achievement: Wand of Building Granted for: Looting the
 * {@link ModItems#buildTool}
 *
 * @author Isfirs
 * @since 0.2
 */
public class AchWandOfBuilding extends AbstractAchievement
{

    /**
     * Constructor
     * 
     * @param id
     * @param name
     * @param offsetX
     * @param offsetY
     */
    public AchWandOfBuilding(final String id, final String name, final int offsetX, final int offsetY)
    {
        super(id, name, offsetX, offsetY, ModItems.buildTool, ModAchievements.achGetSupply);
    }
}
