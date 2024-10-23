package util;
import java.util.Iterator;
import java.util.NoSuchElementException;
/**
 * Generic List class that provides dynamic array functionality for storing objects.
 * @author Terry Nguyen, Olivia Schroeder
 */
public class List<E> implements Iterable<E> {
    private E[] objects;
    private int size;
    /**
     * default constructor with an initial capacity of 4
     */
    public List() {
        this.objects = (E[]) new Object[4];
        this.size = 0;
    }
    /**
     * Increases the size of the array of Appointment objects by 4
     */
    private void grow() {
        E[] newArray = (E[]) new Object[this.objects.length + 4];
        for (int i = 0; i < this.objects.length; i++) {
            newArray[i] = this.objects[i]; // copy elements over to newArray
        }
        this.objects = newArray;
    }
    /**
     * @param e, the object we want to check if is in the list
     * @return true if the object was found in the list, false otherwise
     */
    public boolean contains(E e) {
        for (int i = 0; i < this.size; i++) {
            if (this.objects[i] != null && this.objects[i].equals(e)) {
                return true;
            }
        }
        return false;
    }
    /**
     * add an object to the list, this increases size of list if needed
     * @param e the object to add
     */
    public void add(E e) {
        if (this.size == this.objects.length) {grow();}
        this.objects[this.size] = e; // add the object to the end of the list
        this.size++;
    }
    /**
     * remove an object from the list, shifts all elements to the left
     * @param e the object to remove
     */
    public void remove(E e) {
        int index = find(e);
        if (index == -1) {return;}
        for (int i = index; i < this.size - 1; i++) {
            this.objects[i] = this.objects[i + 1];
        }
        this.objects[this.size - 1] = null; // Nullify the last element
        this.size--;
    }
    /**
     * @return true if the list is empty, false otherwise
     */
    public boolean isEmpty() {return this.size == 0;}
    /**
     * @return the number of elements in the list
     */
    public int size() {return this.size;}
    /**
     * @return an iterator over the elements in the list
     */
    public Iterator<E> iterator() {
        return new ListIterator(); // return new ListIterator instance
    }
    /**
     * @param index the index of the object to get
     * @return the object at the index
     */
    public E get(int index) {return this.objects[index];}
    /**
     * @param index to set the object at
     * @param e the object to set at the index
     */
    public void set(int index, E e) {this.objects[index] = e;}
    /**
     * find the index of the object in the list
     * @param e the object to find the index of
     * @return the index of the object e in the list
     */
    private int find(E e) {
        for (int i = 0; i < this.size; i++) {
            if (this.objects[i].equals(e)) {
                return i;
            }
        }
        return -1;
    }
    /**
     * get the index of the object or return -1 if not found
        * @param e the object to find the index of
        * @return the index of the object e in the list, or -1 if not found
     */
    public int indexOf(E e) {
        for (int i = 0; i < this.size; i++) {
            if (this.objects[i].equals(e)) {
                return i;
            }
        }
        return -1;
    }
    /**
     * subclass of List, get the object at the index or return null if not found
     */
    private class ListIterator<E> implements Iterator<E> {
        private int currentIndex = 0; // Initialize currentIndex
        /**
         * @return true if the iteration has more elements, false otherwise (its empty, or end of list)
         */
        public boolean hasNext() {
            return currentIndex < size;
        }
        /**
         * @return the next element, E, in the iteration
         */
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return (E) objects[currentIndex++];
        }
    }
}