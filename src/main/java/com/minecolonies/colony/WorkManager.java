package com.minecolonies.colony;

import com.minecolonies.colony.workorders.AbstractWorkOrder;
import com.minecolonies.util.Log;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles work orders for a colony.
 */
public class WorkManager
{
    protected final Colony colony;

    private Map<Integer, AbstractWorkOrder> workOrders      = new HashMap<>();
    private int                     topWorkOrderId = 0;

    private static  final   String                  TAG_WORK_ORDERS                 = "workOrders";

    //  Once a second
    private static  final   int                     WORK_ORDER_FULFILL_INCREMENT    = 1 * 20;

    /**
     * Constructor, saves reference to the colony.
     *
     * @param c Colony the work manager is for.
     */
    public WorkManager(Colony c)
    {
        colony = c;
    }

    /**
     * Adds work order to the work manager
     *
     * @param order    Order to add
     */
    public void addWorkOrder(AbstractWorkOrder order)
    {
        if (order.getID() == 0)
        {
            topWorkOrderId++;
            order.setID(topWorkOrderId);
        }

        workOrders.put(order.getID(), order);
    }

    /**
     * Removes a work order from the work manager
     *
     * @param orderId   ID of the order to remove
     */
    public void removeWorkOrder(int orderId)
    {
        workOrders.remove(orderId);
    }

    /**
     * Removes a work order from the work manager
     *
     * @param order     {@link AbstractWorkOrder} to remove
     */
    public void removeWorkOrder(AbstractWorkOrder order)
    {
        removeWorkOrder(order.getID());
    }

    /**
     * Get a work order of the specified id
     *
     * @param id        the id of the work order
     * @return          the work order of the specified id, or null
     */
    public AbstractWorkOrder getWorkOrder(int id)
    {
        return workOrders.get(id);
    }

    /**
     * Get a work order of the specified id, as a specific type
     *
     * @param id        the id of the work order
     * @param type      the class of the expected type of the work order
     * @return          the work order of the specified id, or null if it was not found or is of an incompatible type
     */
    public <W extends AbstractWorkOrder> W getWorkOrder(int id, Class<W> type)
    {
        try
        {
            return type.cast(getWorkOrder(id));
        }
        catch (ClassCastException exc)
        {
            Log.logger.catching(exc);
        }

        return null;
    }

    /**
     * Get an unclaimed work order of a specified type
     *
     * @param type      the class of the type of work order to find
     * @return          an unclaimed work order of the given type, or null if no unclaimed work order of the type was found
     */
    public <W extends AbstractWorkOrder> W getUnassignedWorkOrder(Class<W> type)
    {
        for (AbstractWorkOrder o : workOrders.values())
        {
            if (!o.isClaimed() && type.isAssignableFrom(o.getClass()))
            {
                return type.cast(o);
            }
        }

        return null;
    }

    /**
     * Get all work orders of a specified type
     *
     * @param type the class of the type of work order to find
     * @return a list of all work orders of the given type
     */
    public <W extends AbstractWorkOrder> List<W> getWorkOrdersOfType(Class<W> type)
    {
        return workOrders.values().stream().filter(o -> type.isAssignableFrom(o.getClass())).map(type::cast).collect(Collectors.toList());
    }

    /**
     * When a citizen is removed, unclaim any Work Orders that were claimed by that citizen
     *
     * @param citizen       Citizen to unclaim work for.
     */
    public void clearWorkForCitizen(CitizenData citizen)
    {
        workOrders.values().stream().filter(o -> o.isClaimedBy(citizen)).forEach(AbstractWorkOrder::clearClaimedBy);
    }

    /**
     * Save the Work Manager
     *
     * @param compound      Compound to save to
     */
    public void writeToNBT(NBTTagCompound compound)
    {
        //  Work Orders
        NBTTagList list = new NBTTagList();
        for (AbstractWorkOrder o : workOrders.values())
        {
            NBTTagCompound orderCompound = new NBTTagCompound();
            o.writeToNBT(orderCompound);
            list.appendTag(orderCompound);
        }
        compound.setTag(TAG_WORK_ORDERS, list);
    }

    /**
     * Restore the Work Manager
     *
     * @param compound      Compound to read from
     */
    public void readFromNBT(NBTTagCompound compound)
    {
        //  Work Orders
        NBTTagList list = compound.getTagList(TAG_WORK_ORDERS, NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); ++i)
        {
            NBTTagCompound orderCompound = list.getCompoundTagAt(i);
            AbstractWorkOrder o = AbstractWorkOrder.createFromNBT(orderCompound);
            if (o != null)
            {
                addWorkOrder(o);

                //  If this Work Order is claimed, and the Citizen who claimed it no longer exists
                //  then clear the Claimed status
                //  This is just a failsafe cleanup; this should not happen under normal circumstances
                if (o.isClaimed() && colony.getCitizen(o.getClaimedBy()) == null)
                {
                    o.clearClaimedBy();
                }

                topWorkOrderId = Math.max(topWorkOrderId, o.getID());
            }
        }
    }

    /**
     * Process updates on the World Tick
     * Currently, does periodic Work Order cleanup
     *
     * @param event         {@link net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent}
     */
    public void onWorldTick(TickEvent.WorldTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END)
        {
            Iterator<AbstractWorkOrder> iter = workOrders.values().iterator();
            while (iter.hasNext())
            {
                AbstractWorkOrder o = iter.next();
                if (!o.isValid(colony))
                {
                    iter.remove();
                }
            }

            if ((event.world.getWorldTime() % WORK_ORDER_FULFILL_INCREMENT) == 0)
            {
                workOrders.values().stream().filter(o -> !o.isClaimed()).forEach(o -> o.attemptToFulfill(colony));
            }
        }
    }
}
