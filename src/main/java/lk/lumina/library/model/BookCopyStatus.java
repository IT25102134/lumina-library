package lk.lumina.library.model;
// Catalogue availability treats only AVAILABLE physical copies as requestable; the other values represent non-available inventory states.
public enum BookCopyStatus { AVAILABLE, LOANED, RESERVED, DAMAGED, LOST }
